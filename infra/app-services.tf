locals {
  image_prefix = "ghcr.io/aaysha-s/flashcart-"
  dns          = "${var.project}.local"
  db_url       = "jdbc:postgresql://${aws_db_instance.main.address}:5432"

  common_env = {
    SPRING_KAFKA_BOOTSTRAP_SERVERS          = "kafka.${local.dns}:9092"
    MANAGEMENT_TRACING_SAMPLING_PROBABILITY = "0" # no Jaeger in AWS
  }

  apps = {
    auth = {
      image   = "auth-service"
      port    = 8081
      db      = "auth_db"
      env     = {}
      secrets = { JWT_SECRET = aws_ssm_parameter.jwt_secret.arn }
    }
    inventory = {
      image = "inventory-service"
      port  = 8082
      db    = "inventory_db"
      env   = { SPRING_DATA_REDIS_HOST = "redis.${local.dns}" }
      secrets = {
        JWT_SECRET     = aws_ssm_parameter.jwt_secret.arn
        GEMINI_API_KEY = aws_ssm_parameter.gemini_api_key.arn
      }
    }
    order = {
      image = "order-service"
      port  = 8083
      db    = "order_db"
      env   = { INVENTORY_SERVICE_URL = "http://inventory.${local.dns}:8082" }
      secrets = {
        JWT_SECRET     = aws_ssm_parameter.jwt_secret.arn
        GEMINI_API_KEY = aws_ssm_parameter.gemini_api_key.arn
      }
    }
    gateway = {
      image = "gateway"
      port  = 8080
      db    = null
      env = {
        AUTH_SERVICE_URL      = "http://auth.${local.dns}:8081"
        INVENTORY_SERVICE_URL = "http://inventory.${local.dns}:8082"
        ORDER_SERVICE_URL     = "http://order.${local.dns}:8083"
      }
      secrets = {}
    }
  }
}

resource "aws_service_discovery_service" "app" {
  for_each = local.apps
  name     = each.key
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      type = "A"
      ttl  = 10
    }
  }
}

resource "aws_ecs_task_definition" "app" {
  for_each                 = local.apps
  family                   = "${var.project}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([{
    name         = each.key
    image        = "${local.image_prefix}${each.value.image}:latest"
    essential    = true
    portMappings = [{ containerPort = each.value.port }]

    environment = [for k, v in merge(
      local.common_env,
      each.value.env,
      each.value.db == null ? {} : {
        SPRING_DATASOURCE_URL      = "${local.db_url}/${each.value.db}?sslmode=require"
        SPRING_DATASOURCE_USERNAME = "flashcart"
      }
    ) : { name = k, value = v }]

    secrets = [for k, v in merge(
      each.value.secrets,
      each.value.db == null ? {} : { SPRING_DATASOURCE_PASSWORD = aws_ssm_parameter.db_password.arn }
    ) : { name = k, valueFrom = v }]

    logConfiguration = local.log_config
  }])
}

resource "aws_ecs_service" "app" {
  for_each        = local.apps
  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.public_subnets
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.app[each.key].arn
  }

  dynamic "load_balancer" {
    for_each = each.key == "gateway" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.gateway.arn
      container_name   = "gateway"
      container_port   = 8080
    }
  }

  health_check_grace_period_seconds = each.key == "gateway" ? 120 : null

  deployment_circuit_breaker {
    enable   = true
    rollback = false
  }
}