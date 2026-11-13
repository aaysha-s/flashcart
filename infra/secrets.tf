resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project}/jwt-secret"
  type  = "SecureString"
  value = var.jwt_secret
}

resource "aws_ssm_parameter" "gemini_api_key" {
  name  = "/${var.project}/gemini-api-key"
  type  = "SecureString"
  value = var.gemini_api_key
}