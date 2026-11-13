variable "region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "ap-southeast-2"
}

variable "project" {
  description = "Name prefix for every resource"
  type        = string
  default     = "flashcart"
}

variable "jwt_secret" {
  description = "Base64 HMAC key shared by all services"
  type        = string
  sensitive   = true
}

variable "gemini_api_key" {
  description = "Google Gemini API key"
  type        = string
  sensitive   = true
}