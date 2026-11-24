variable "region" {
  type    = string
  default = "ap-southeast-2"
}

variable "node_instance_type" {
  type    = string
  default = "m7i-flex.large" # free-tier eligible: 2 vCPU, 8 GB
}