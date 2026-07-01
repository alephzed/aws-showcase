# Image tags for the ECS services (added by issue #1). Passed at apply time,
# e.g. -var scheduling_image_tag=<git-sha> -var notification_image_tag=<git-sha>.

variable "scheduling_image_tag" {
  description = "Container image tag for the Scheduling service in its ECR repo."
  type        = string
}

variable "notification_image_tag" {
  description = "Container image tag for the Notification service in its ECR repo."
  type        = string
}

variable "service_cpu" {
  description = "Fargate task CPU units per service."
  type        = number
  default     = 512
}

variable "service_memory" {
  description = "Fargate task memory (MiB) per service."
  type        = number
  default     = 1024
}
