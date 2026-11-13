output "vpc_id" {
  value = data.aws_vpc.default.id
}

output "subnet_ids" {
  value = data.aws_subnets.public.ids
}

output "db_endpoint" {
  value = aws_db_instance.main.address
}