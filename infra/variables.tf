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