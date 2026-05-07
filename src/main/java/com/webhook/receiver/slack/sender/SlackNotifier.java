package com.webhook.receiver.slack.sender;

import com.webhook.receiver.slack.vo.sender.Field;
import com.webhook.receiver.slack.vo.sender.SlackAttachment;
import com.webhook.receiver.slack.vo.sender.SlackPayload;
import com.webhook.receiver.slack.vo.UserMember;
import com.webhook.receiver.slack.vo.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SlackNotifier implements Notifier {

    private final Logger logger = LoggerFactory.getLogger(SlackNotifier.class.getName());
    private final RestTemplate restTemplate;
    private final Map<String, String> webhookUrlMap;

    @Autowired
    public SlackNotifier(RestTemplate restTemplate,
            @Value("${SLACK_WEBHOOK_URL_KICS_PROD:}") final String kicsProdUrl,
            @Value("${SLACK_WEBHOOK_URL_API_HUB_PRD:}") final String apiHubPrdUrl,
            @Value("${SLACK_WEBHOOK_URL_RESULT_HUB_PROD:}") final String resultHubProdUrl,
            @Value("${SLACK_WEBHOOK_URL_AGREE_PROD:}") final String agreeProdUrl,
            @Value("${SLACK_WEBHOOK_URL_MIS_PROD:}") final String misProdUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrlMap = new HashMap<>();
        if (!kicsProdUrl.isEmpty()) webhookUrlMap.put("kics-prod", kicsProdUrl);
        if (!apiHubPrdUrl.isEmpty()) webhookUrlMap.put("api-hub-prd", apiHubPrdUrl);
        if (!resultHubProdUrl.isEmpty()) webhookUrlMap.put("result-hub-prod", resultHubProdUrl);
        if (!agreeProdUrl.isEmpty()) webhookUrlMap.put("agree-prod", agreeProdUrl);
        if (!misProdUrl.isEmpty()) webhookUrlMap.put("mis-prod", misProdUrl);
    }

    public boolean send(WebhookPayload webhookPayload) {
        String targetUrl = webhookUrlMap.get(webhookPayload.getApplicationId());
        if (targetUrl == null) {
            logger.warn("No webhook URL configured for applicationId={}", webhookPayload.getApplicationId());
            return false;
        }
        List<SlackAttachment> attachments = buildAttachments(webhookPayload);
        SlackPayload slackPayload = new SlackPayload(attachments);

        try {
            restTemplate.postForObject(new URI(targetUrl), slackPayload, String.class);
            logger.info("Sent slack message to {} (applicationId={})", targetUrl, webhookPayload.getApplicationId());
        } catch (Exception e) {
            logger.error("Failed to send Slack message for applicationId={}",
                        webhookPayload.getApplicationId(), e);
            return false;
        }
        return true;
    }
    
    List<SlackAttachment> buildAttachments(WebhookPayload webhookPayload) {
        SlackAttachment slackAttachment = new SlackAttachment();

        String pinpointLink = buildPinpointLink(webhookPayload);
        if (pinpointLink != null) {
            slackAttachment.setTitle("Pinpoint 바로가기");
            slackAttachment.setTitle_link(pinpointLink);
        }

        Field checkerNameField = new Field("알람 유형", webhookPayload.getChecker().getName());
        Field applicationIdField = new Field("애플리케이션", webhookPayload.getApplicationId());
        Field serviceTypeField = new Field("서비스 타입", webhookPayload.getServiceType());
        Field sequenceCountField = new Field("연속 감지 횟수", webhookPayload.getSequenceCount().toString());
        Field unitField = new Field("단위", webhookPayload.getUnit());
        Field thresholdField = new Field("임계값", webhookPayload.getThreshold().toString());

        Field detectedValue = new Field("감지된 값", webhookPayload.getChecker().getDetectedValueString());
        Field envField = new Field("환경", webhookPayload.getBatchEnv());

        final StringBuilder owners = new StringBuilder();
        if (webhookPayload.getUserGroup() != null) {
            List<UserMember> userMembers = webhookPayload.getUserGroup().getUserGroupMembers();
            if (userMembers != null) {
                for(UserMember userMember : userMembers) {
                    owners.append("@").append(userMember.getId()).append("\n");
                }
            }
        }
        Field ownerField = new Field("담당자", owners.toString());

        slackAttachment.addField(checkerNameField);
        slackAttachment.addField(envField);
        slackAttachment.addField(applicationIdField);
        slackAttachment.addField(serviceTypeField);
        slackAttachment.addField(sequenceCountField);
        slackAttachment.addField(unitField);
        slackAttachment.addField(thresholdField);
        slackAttachment.addField(detectedValue);
        slackAttachment.addField(ownerField);
        if (webhookPayload.getNotes() != null && !webhookPayload.getNotes().isEmpty()) {
            slackAttachment.addField(new Field("메모", webhookPayload.getNotes()));
        }
    
        List<SlackAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(slackAttachment);
        return attachmentList;
    }

    private String buildPinpointLink(WebhookPayload payload) {
        String baseUrl = payload.getPinpointUrl();
        if (baseUrl == null || baseUrl.isEmpty()) return null;

        String appTarget = payload.getApplicationId() + "@" + payload.getServiceType();
        String checkerName = payload.getChecker().getName().toUpperCase();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        String from = now.minusMinutes(5).format(fmt);
        String to = now.format(fmt);

        if (checkerName.contains("ERROR") || checkerName.contains("DEADLOCK")) {
            return baseUrl + "/errorAnalysis/" + appTarget + "?from=" + from + "&to=" + to;
        } else if (checkerName.contains("JVM") || checkerName.contains("CPU")
                || checkerName.contains("HEAP") || checkerName.contains("GC")
                || checkerName.contains("FILE") || checkerName.contains("DATASOURCE")) {
            return baseUrl + "/inspector/" + appTarget + "?from=" + from + "&to=" + to;
        } else {
            return baseUrl + "/serverMap/" + appTarget + "?from=" + from + "&to=" + to;
        }
    }
}
