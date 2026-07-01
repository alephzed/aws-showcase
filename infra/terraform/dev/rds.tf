resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = aws_subnet.public[*].id

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

resource "random_password" "db" {
  length  = 24
  special = true
  # Exclude characters that are awkward in PostgreSQL connection URIs.
  override_special = "!#$%*-_=+"
}

# Cost-minimized: db.t3.micro, single-AZ, gp3 20GB, not publicly accessible.
resource "aws_db_instance" "main" {
  identifier     = "${local.name_prefix}-postgres"
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  multi_az               = false
  publicly_accessible    = false
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 0
  deletion_protection     = false
  skip_final_snapshot     = true
  apply_immediately       = true

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}

# Store the full connection secret in Secrets Manager for the services to read.
resource "aws_secretsmanager_secret" "db" {
  name                    = "${local.name_prefix}/rds/scheduling"
  description             = "PostgreSQL connection credentials for the Scheduling database (dev)."
  recovery_window_in_days = 0

  tags = {
    Name = "${local.name_prefix}-rds-secret"
  }
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id

  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    engine   = "postgres"
    host     = aws_db_instance.main.address
    port     = aws_db_instance.main.port
    dbname   = var.db_name
    jdbc_url = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${var.db_name}"
  })
}
