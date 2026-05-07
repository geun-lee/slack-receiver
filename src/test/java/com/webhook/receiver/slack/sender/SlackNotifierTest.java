package com.webhook.receiver.slack.sender;

import com.webhook.receiver.slack.vo.sender.SlackPayload;
import com.webhook.receiver.slack.vo.LongValueAlarmChecker;
import com.webhook.receiver.slack.vo.UserGroup;
import com.webhook.receiver.slack.vo.WebhookPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackNotifierTest {

    @Mock
    RestTemplate restTemplate;

    private SlackNotifier slackNotifier;

    @Test
    void sendFailWhenApplicationIdNotConfigured() {
        slackNotifier = new SlackNotifier(restTemplate, "http://webhook.kics-prod.com", "", "", "", "");

        WebhookPayload payload = new WebhookPayload("pinpointUrl", "release", "unknown-app", "serviceType", new UserGroup("userGroupId", new ArrayList<>()), new LongValueAlarmChecker(1L), "%", 1, "", 1);

        boolean result = slackNotifier.send(payload);
        Assertions.assertFalse(result);
    }

    @Test
    void sendFailWhenRestClientException() {
        lenient().when(restTemplate.postForObject(any(URI.class), any(SlackPayload.class), eq(String.class))).thenThrow(RestClientException.class);

        slackNotifier = new SlackNotifier(restTemplate, "http://webhook.kics-prod.com", "", "", "", "");

        WebhookPayload payload = new WebhookPayload("pinpointUrl", "release", "kics-prod", "serviceType", new UserGroup("userGroupId", new ArrayList<>()), new LongValueAlarmChecker(1L), "%", 1, "", 1);

        boolean result = slackNotifier.send(payload);
        Assertions.assertFalse(result);
    }

    @Test
    void sendSuccess() {
        when(restTemplate.postForObject(any(URI.class), any(SlackPayload.class), eq(String.class))).thenReturn("");
        slackNotifier = new SlackNotifier(restTemplate, "http://webhook.kics-prod.com", "http://webhook.api-hub-prd.com", "http://webhook.result-hub-prod.com", "", "");

        WebhookPayload payload = new WebhookPayload("pinpointUrl", "release", "kics-prod", "serviceType", new UserGroup("userGroupId", new ArrayList<>()), new LongValueAlarmChecker(1L), "%", 1, "", 1);

        boolean result = slackNotifier.send(payload);
        Assertions.assertTrue(result);
    }

    @Test
    void sendSuccessWithMultipleApplicationIds() {
        when(restTemplate.postForObject(any(URI.class), any(SlackPayload.class), eq(String.class))).thenReturn("");
        slackNotifier = new SlackNotifier(restTemplate, "http://webhook.kics-prod.com", "http://webhook.api-hub-prd.com", "http://webhook.result-hub-prod.com", "", "");

        WebhookPayload payload = new WebhookPayload("pinpointUrl", "release", "api-hub-prd", "serviceType", new UserGroup("userGroupId", new ArrayList<>()), new LongValueAlarmChecker(1L), "%", 1, "", 1);

        boolean result = slackNotifier.send(payload);
        Assertions.assertTrue(result);
    }
    
}