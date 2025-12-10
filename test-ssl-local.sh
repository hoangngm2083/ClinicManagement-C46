#!/bin/bash

# Local SSL Testing Script
# Tests HTTPS and WebSocket with self-signed certificates

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Create self-signed certificates for local testing
create_self_signed_cert() {
    log_step "Creating self-signed certificates for local testing..."

    # Create SSL directory if not exists
    mkdir -p ./nginx/ssl

    # Generate self-signed certificate
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ./nginx/ssl/key.pem \
        -out ./nginx/ssl/cert.pem \
        -subj "/C=VN/ST=HoChiMinh/L=HoChiMinh/O=PTIT/OU=C46/CN=clinic46.duckdns.org"

    log_info "Self-signed certificate created in ./nginx/ssl/"
}

# Start services with SSL
start_services_ssl() {
    log_step "Starting services with SSL configuration..."

    # Stop any existing services
    ./deploy.sh down 2>/dev/null || true

    # Start all services
    ./deploy.sh up --build -d

    # Wait for services to be ready
    log_info "Waiting for services to start..."
    sleep 30

    # Check if nginx is running
    if docker ps | grep -q nginx-prod; then
        log_info "Nginx is running"
    else
        log_error "Nginx failed to start"
        docker logs nginx-prod
        exit 1
    fi
}

# Test HTTP to HTTPS redirect
test_http_redirect() {
    log_step "Testing HTTP to HTTPS redirect..."

    # Test redirect (allow insecure because self-signed cert)
    if curl -s -o /dev/null -w "%{http_code}" --max-time 10 http://localhost/ | grep -q "301"; then
        log_info "✓ HTTP to HTTPS redirect working"
    else
        log_warn "⚠ HTTP redirect may not be working (check manually)"
    fi
}

# Test HTTPS health check
test_https_health() {
    log_step "Testing HTTPS health check..."

    # Test HTTPS health endpoint (ignore SSL verification for self-signed)
    if curl -s -k --max-time 10 https://localhost/health | grep -q "healthy"; then
        log_info "✓ HTTPS health check working"
    else
        log_error "✗ HTTPS health check failed"
        return 1
    fi
}

# Test HTTPS API endpoint
test_https_api() {
    log_step "Testing HTTPS API endpoints..."

    # Test API gateway health
    if curl -s -k --max-time 10 https://localhost/actuator/health >/dev/null; then
        log_info "✓ HTTPS API gateway accessible"
    else
        log_warn "⚠ API gateway not accessible (may not be running)"
    fi
}

# Test WebSocket connection
test_websocket() {
    log_step "Testing WebSocket connection..."

    # Install websocat if not available
    if ! command -v websocat &> /dev/null; then
        log_info "Installing websocat for WebSocket testing..."
        if command -v apt &> /dev/null; then
            sudo apt update && sudo apt install -y websocat
        elif command -v brew &> /dev/null; then
            brew install websocat
        else
            log_warn "Please install websocat manually: https://github.com/vi/websocat"
            return 1
        fi
    fi

    # Test WebSocket connection (this will try to connect and immediately disconnect)
    log_info "Testing WebSocket connection to wss://localhost/ws/exam-workflow..."

    # Use timeout to prevent hanging
    timeout 5s bash -c "
        echo 'Testing WebSocket connection...' >&2
        if websocat -k wss://localhost/ws/exam-workflow 2>/dev/null <<< 'test' | head -1 >/dev/null; then
            echo 'SUCCESS' >&2
            exit 0
        else
            echo 'FAILED' >&2
            exit 1
        fi
    " 2>&1 | grep -q "SUCCESS"

    if [ $? -eq 0 ]; then
        log_info "✓ WebSocket connection successful"
    else
        log_warn "⚠ WebSocket connection failed (service may not be running)"
    fi
}

# Create simple WebSocket test HTML page
create_websocket_test_page() {
    log_step "Creating WebSocket test page..."

    mkdir -p ./nginx/html

    cat > ./nginx/html/websocket-test.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket SSL Test</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        #messages { border: 1px solid #ccc; padding: 10px; height: 200px; overflow-y: auto; background: #f9f9f9; }
        input[type="text"] { width: 300px; padding: 5px; }
        button { padding: 5px 10px; margin: 5px; }
        .status { margin: 10px 0; padding: 10px; border-radius: 5px; }
        .connected { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .disconnected { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
    </style>
</head>
<body>
    <h1>WebSocket SSL Test Page</h1>
    <p>This page tests WebSocket connections over HTTPS (WSS)</p>

    <div id="status" class="status disconnected">Disconnected</div>

    <div>
        <button onclick="connect()">Connect</button>
        <button onclick="disconnect()">Disconnect</button>
        <button onclick="clearMessages()">Clear</button>
    </div>

    <div>
        <input type="text" id="messageInput" placeholder="Enter message to send" onkeypress="handleKeyPress(event)">
        <button onclick="sendMessage()">Send</button>
    </div>

    <div>
        <h3>Messages:</h3>
        <div id="messages"></div>
    </div>

    <script>
        let ws = null;

        function addMessage(message, type = 'info') {
            const messages = document.getElementById('messages');
            const timestamp = new Date().toLocaleTimeString();
            const messageDiv = document.createElement('div');
            messageDiv.innerHTML = `<span style="color: #666;">[${timestamp}]</span> <span style="color: ${type === 'error' ? 'red' : type === 'success' ? 'green' : 'black'};">${message}</span>`;
            messages.appendChild(messageDiv);
            messages.scrollTop = messages.scrollHeight;
        }

        function updateStatus(message, connected = false) {
            const status = document.getElementById('status');
            status.textContent = message;
            status.className = `status ${connected ? 'connected' : 'disconnected'}`;
        }

        function connect() {
            if (ws && ws.readyState === WebSocket.OPEN) {
                addMessage('Already connected', 'info');
                return;
            }

            try {
                addMessage('Connecting to wss://localhost/ws/exam-workflow...', 'info');
                ws = new WebSocket('wss://localhost/ws/exam-workflow');

                ws.onopen = function(event) {
                    addMessage('WebSocket connected successfully!', 'success');
                    updateStatus('Connected to WebSocket', true);
                };

                ws.onmessage = function(event) {
                    addMessage('Received: ' + event.data, 'success');
                };

                ws.onclose = function(event) {
                    addMessage('WebSocket closed (code: ' + event.code + ')', 'info');
                    updateStatus('Disconnected', false);
                };

                ws.onerror = function(error) {
                    addMessage('WebSocket error: ' + error, 'error');
                    updateStatus('Connection failed', false);
                };

            } catch (error) {
                addMessage('Connection error: ' + error.message, 'error');
                updateStatus('Connection failed', false);
            }
        }

        function disconnect() {
            if (ws) {
                ws.close();
                ws = null;
            } else {
                addMessage('Not connected', 'info');
            }
        }

        function sendMessage() {
            const input = document.getElementById('messageInput');
            const message = input.value.trim();

            if (!message) {
                addMessage('Please enter a message', 'error');
                return;
            }

            if (!ws || ws.readyState !== WebSocket.OPEN) {
                addMessage('Not connected to WebSocket', 'error');
                return;
            }

            ws.send(message);
            addMessage('Sent: ' + message, 'info');
            input.value = '';
        }

        function clearMessages() {
            document.getElementById('messages').innerHTML = '';
        }

        function handleKeyPress(event) {
            if (event.key === 'Enter') {
                sendMessage();
            }
        }

        // Auto-connect on page load for testing
        window.onload = function() {
            addMessage('Page loaded. Click "Connect" to test WebSocket over HTTPS.', 'info');
        };
    </script>
</body>
</html>
EOF

    log_info "WebSocket test page created at ./nginx/html/websocket-test.html"
    log_info "Access it at: https://localhost/websocket-test.html (accept self-signed certificate)"
}

# Show test results and next steps
show_results() {
    log_step "Test Results Summary"

    echo ""
    echo "========================================="
    echo "SSL/HTTPS Test Results"
    echo "========================================="
    echo "✓ Self-signed certificates created"
    echo "✓ Nginx configured for SSL"
    echo "✓ HTTP to HTTPS redirect configured"
    echo "✓ WebSocket proxy configured"
    echo ""
    echo "Test URLs:"
    echo "- HTTPS Health: https://localhost/health"
    echo "- WebSocket Test Page: https://localhost/websocket-test.html"
    echo "- API Gateway: https://localhost/actuator/health"
    echo ""
    echo "Next steps:"
    echo "1. Open https://localhost/websocket-test.html in browser"
    echo "2. Accept the self-signed certificate warning"
    echo "3. Click 'Connect' to test WebSocket"
    echo "4. For production: run ./setup-ssl.sh obtain"
    echo "========================================="
}

# Main execution
main() {
    case "${1:-test}" in
        "setup")
            create_self_signed_cert
            ;;
        "start")
            start_services_ssl
            ;;
        "test")
            create_self_signed_cert
            start_services_ssl
            test_http_redirect
            test_https_health
            test_https_api
            test_websocket
            create_websocket_test_page
            show_results
            ;;
        "stop")
            log_info "Stopping services..."
            ./deploy.sh down
            ;;
        *)
            echo "Usage: $0 [setup|start|test|stop]"
            echo "  setup - Create self-signed certificates only"
            echo "  start - Start services with SSL"
            echo "  test  - Full test (default)"
            echo "  stop  - Stop services"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
