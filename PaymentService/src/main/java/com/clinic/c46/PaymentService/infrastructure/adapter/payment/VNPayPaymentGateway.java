package com.clinic.c46.PaymentService.infrastructure.adapter.payment;

import com.clinic.c46.PaymentService.application.service.PaymentGateway;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import com.clinic.c46.PaymentService.infrastructure.config.payment.vnpay.VNPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Component
public class VNPayPaymentGateway implements PaymentGateway {

    private final VNPayConfig vNPayConfig;

    @Override
    public String generateURL(String transactionId, BigDecimal requestAmount, String clientIp) {
        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vNPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", requestAmount.multiply(BigDecimal.valueOf(100))
                .toBigInteger()
                .toString());
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transactionId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoa don kham suc khoe");

        vnp_Params.put("vnp_OrderType", "other");

        vnp_Params.put("vnp_Locale", "vn");

        // QUAN TRỌNG: Xử lý IP Address
        // Nếu chạy local, clientIp có thể là IPv6 (0:0:0:0:0:0:0:1) -> VNPay từ chối
        if (clientIp == null || clientIp.isEmpty() || clientIp.contains(":")) {
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        } else {
            vnp_Params.put("vnp_IpAddr", clientIp);
        }

        vnp_Params.put("vnp_ReturnUrl", vNPayConfig.getReturnUrl() + "?gateway=" + PaymentMethod.VNPAY.name());

        // Đồng bộ Timezone về Vietnam (GMT+7)
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        String vnp_CreateDate = now.format(formatter);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        String vnp_ExpireDate = now.plusMinutes(vNPayConfig.getExpireMinutes())
                .format(formatter);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build data to hash and querystring
        Map<String, String> queryAndHashData = hashing(vnp_Params);
        String queryUrl = queryAndHashData.get("query");
        String vnp_SecureHash = vNPayConfig.hmacSHA512(vNPayConfig.getSecretKey(), queryAndHashData.get("hashData"));
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vNPayConfig.getPayUrl() + "?" + queryUrl;
    }


    @Override
    public CompletableFuture<WebhookResult> handleWebhook(Map<String, String> params) {
        // 1. Validate (CPU-bound task -> chạy trực tiếp, không cần supplyAsync)
        if (!isValidChecksum(params)) {
            log.error("Checksum failed for params: {}", params);
            return CompletableFuture.completedFuture(WebhookResult.invalid(params));
        }

        // 2. Extract Data
        String txnRef = params.get("vnp_TxnRef");
        String transNo = params.get("vnp_TransactionNo");
        boolean isSuccess = "00".equals(params.get("vnp_ResponseCode"));

        // 3. Return Result (Wrapped in Future)
        return CompletableFuture.completedFuture(WebhookResult.success(txnRef, transNo, isSuccess, params));
    }

    private boolean isValidChecksum(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null) return false;

        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHashType");
        sorted.remove("vnp_SecureHash");

        // Lưu ý: Hàm hashing của bạn nên trả về chuỗi hashData thô để clean hơn,
        // nhưng ở đây tôi giữ logic cũ của bạn
        String hashData = hashing(sorted).get("hashData");
        String calculated = vNPayConfig.hmacSHA512(vNPayConfig.getSecretKey(), hashData);

        return secureHash.equals(calculated);
    }


    @Override
    public void refund(Object transactionId) {
        log.info("Processing refund for VNPay transaction: {}", transactionId);
    }

    private Map<String, String> hashing(Map<String, String> vnp_Params) {
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                try {
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);

                    //Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(encodedValue); // VNPay Sandbox yêu cầu hash giá trị đã encode

                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                    query.append('=');
                    query.append(encodedValue);

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    log.error("Error encoding URL", e);
                    throw e;
                }
            }
        }
        Map<String, String> result = new HashMap<>();
        result.put("hashData", hashData.toString());
        result.put("query", query.toString());
        return result;
    }


}