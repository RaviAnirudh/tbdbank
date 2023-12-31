package org.ece.controllers;

import org.ece.dto.qr.QRGenerateRequest;
import org.ece.dto.qr.QRGenerateResponse;
import org.ece.dto.qr.QRPaymentRequest;
import org.ece.dto.qr.QRPaymentResponse;
import org.ece.service.QRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qr")
public class QRController {
    private QRService qrService;
    private static final Logger logger = LoggerFactory.getLogger(QRController.class);

    public QRController(final QRService qrService) {
        this.qrService = qrService;
    }

    @RequestMapping(value = "/generateQR", method = RequestMethod.POST)
    public ResponseEntity<QRGenerateResponse> generateUpiQR(@RequestBody QRGenerateRequest qrGenerateRequest) {
        logger.info("Received QR Generate Request");
        final QRGenerateResponse qrGenerateResponse = qrService.generateQRCode(qrGenerateRequest);
        return ResponseEntity.ok(qrGenerateResponse);
    }

    @RequestMapping(value = "/QRPayment", method = RequestMethod.POST)
    public ResponseEntity<QRPaymentResponse> ingestQR(@RequestBody QRPaymentRequest qrPaymentRequest) {
        logger.info("Received QR Payment Request");
        final QRPaymentResponse qrPaymentResponse = qrService.validateQRPaymentRequest(qrPaymentRequest);
        return ResponseEntity.ok(qrPaymentResponse);
    }

}
