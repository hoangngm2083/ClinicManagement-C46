#!/bin/bash

# SSL Certificate Setup for Production
# This script handles SSL certificate acquisition and renewal

set -e

# Configuration
DOMAIN=${DOMAIN:-clinic46.duckdns.org}
EMAIL=${EMAIL:-n21dccn034@student.ptithcm.edu.vn}
STAGING=${STAGING:-0}  # Set to 1 for testing

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running on EC2
check_ec2() {
    if ! curl -s http://169.254.169.254/latest/meta-data/instance-id >/dev/null 2>&1; then
        log_warn "Not running on EC2. Make sure domain points to correct IP."
    fi
}

# Install certbot if not present
install_certbot() {
    if ! command -v certbot &> /dev/null; then
        log_info "Installing certbot..."
        sudo apt update
        sudo apt install -y certbot python3-certbot-nginx

        # Optional: Install AWS Route53 plugin if using AWS
        # pip3 install certbot-dns-route53
    fi
}

# Stop nginx temporarily for standalone mode
stop_services() {
    log_info "Stopping nginx to free port 80..."
    if command -v docker &> /dev/null && docker ps | grep -q nginx; then
        docker stop nginx-prod 2>/dev/null || true
    fi

    # Kill any process using port 80
    sudo fuser -k 80/tcp 2>/dev/null || true
    sleep 2
}

# Start services after certificate acquisition
start_services() {
    log_info "Starting services..."
    ./deploy.sh up -d nginx
}

# Obtain SSL certificate
obtain_certificate() {
    log_info "Obtaining SSL certificate for $DOMAIN..."

    local certbot_cmd="sudo certbot certonly --standalone"

    if [ "$STAGING" = "1" ]; then
        certbot_cmd="$certbot_cmd --staging"
        log_warn "Using Let's Encrypt STAGING environment"
    fi

    $certbot_cmd \
        -d $DOMAIN \
        --email $EMAIL \
        --agree-tos \
        --non-interactive \
        --expand

    if [ $? -eq 0 ]; then
        log_info "Certificate obtained successfully!"

        # Copy actual certificate files to nginx volume (not symlinks)
        sudo mkdir -p ./nginx/ssl
        sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem ./nginx/ssl/cert.pem
        sudo cp /etc/letsencrypt/live/$DOMAIN/privkey.pem ./nginx/ssl/key.pem
        sudo chmod 644 ./nginx/ssl/cert.pem
        sudo chmod 600 ./nginx/ssl/key.pem

        log_info "Certificate files copied to ./nginx/ssl/"
    else
        log_error "Failed to obtain certificate"
        exit 1
    fi
}

# Renew certificate
renew_certificate() {
    log_info "Renewing SSL certificate..."

    # Stop nginx temporarily
    stop_services

    # Renew certificate
    sudo certbot renew

    # Start services
    start_services

    # Reload nginx if running in docker
    if docker ps | grep -q nginx-prod; then
        docker exec nginx-prod nginx -s reload
    fi

    log_info "Certificate renewed successfully!"
}

# Setup automatic renewal
setup_auto_renewal() {
    log_info "Setting up automatic certificate renewal..."

    # Create renewal script
    sudo tee /etc/cron.daily/certbot-renewal >/dev/null <<EOF
#!/bin/bash
sudo certbot renew --quiet
if [ \$? -eq 0 ]; then
    # Reload nginx if running
    docker exec nginx-prod nginx -s reload 2>/dev/null || true
fi
EOF

    sudo chmod +x /etc/cron.daily/certbot-renewal
    log_info "Automatic renewal configured (runs daily)"
}

# Show certificate info
show_cert_info() {
    if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
        log_info "Certificate information:"
        sudo certbot certificates

        log_info "Certificate files:"
        sudo ls -la /etc/letsencrypt/live/$DOMAIN/
    else
        log_warn "No certificate found for $DOMAIN"
    fi
}

# Main execution
main() {
    case "${1:-obtain}" in
        "obtain")
            check_ec2
            install_certbot
            stop_services
            obtain_certificate
            start_services
            setup_auto_renewal
            ;;
        "renew")
            renew_certificate
            ;;
        "info")
            show_cert_info
            ;;
        "test")
            STAGING=1
            check_ec2
            install_certbot
            stop_services
            obtain_certificate
            start_services
            ;;
        *)
            echo "Usage: $0 [obtain|renew|info|test]"
            echo "  obtain - Get new certificate (default)"
            echo "  renew  - Renew existing certificate"
            echo "  info   - Show certificate information"
            echo "  test   - Get staging certificate for testing"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
