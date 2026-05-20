package com.vpoluboyarov.otp.otp.notification;

import com.vpoluboyarov.otp.otp.OtpChannel;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@PropertySource("classpath:sms.properties")
public class SmsNotificationChannel implements NotificationChannel {

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationChannel(
            @Value("${smpp.host}") String host,
            @Value("${smpp.port}") int port,
            @Value("${smpp.system_id}") String systemId,
            @Value("${smpp.password}") String password,
            @Value("${smpp.system_type}") String systemType,
            @Value("${smpp.source_addr}") String sourceAddress) {
        this.host = host;
        this.port = port;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.sourceAddress = sourceAddress;
    }

    @Override
    public OtpChannel type() {
        return OtpChannel.SMS;
    }

    @Override
    public void sendCode(String destination, String code) {
        try (SMPPSession session = new SMPPSession()) {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParameter);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, sourceAddress,
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8)
            );

            log.info("SMS channel: code sent to {}", destination);
        } catch (Exception e) {
            log.error("SMS channel: failed to send to {}", destination, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
}
