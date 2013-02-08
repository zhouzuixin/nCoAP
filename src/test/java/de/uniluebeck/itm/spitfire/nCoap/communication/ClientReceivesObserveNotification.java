package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.*;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
import static de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory.*;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.*;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.*;

/**
 * Tests if a client receives notifications.
 * @author Stefan Hueske
 */
public class ClientReceivesObserveNotification {
    
    private static CoapTestServer testServer = CoapTestServer.getInstance();
    private static CoapTestClient testClient = CoapTestClient.getInstance();
    
    //observable request
    private static CoapRequest request;
    
    //notifications
    private static CoapResponse notification1;
    private static CoapResponse notification2;
    
    
    @BeforeClass
    public static void init() throws Exception {
        testServer.reset();
        testClient.reset();
        
        String requestPath = "/testpath";
        URI targetUri = new URI("coap://localhost:" + COAP_SERVER_PORT + requestPath);
        request = new CoapRequest(CON, GET, targetUri, testClient);
        request.setObserveOptionRequest();
        
        (notification1 = new CoapResponse(CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8"));
        (notification2 = new CoapResponse(CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8"));
        
        testServer.registerDummyService(requestPath);
        testServer.addResponse(notification1, notification2);
        
        //run test sequence
        testClient.writeCoapRequest(request);
        //wait for first notification
        Thread.sleep(300);
        //invoke resource update on server
        testServer.notifyCoapObservers();
        //wait for second notification
        Thread.sleep(300);
    }
    
    @Test
    public void testReceiverReceived2Messages() {
        String message = "Receiver did not receive 2 messages";
        assertEquals(message, 2, testClient.getReceivedResponses().size());
    }
    
    @Test
    public void testReceiverReceivedNotification1() {
        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "1st notification: MsgType is not ACK";
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
        message = "1st notification: Code is not 2.05 (Content)";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
        message = "1st notification: Payload does not match";
        assertEquals(message, notification1.getPayload(), receivedMessage.getPayload());
    }
    
    @Test
    public void testReceiverReceivedNotification2() {
        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        timeKeys.next();
        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
        String message = "1st notification: MsgType is not ACK";
        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
        message = "1st notification: Code is not 2.05 (Content)";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
        message = "1st notification: Payload does not match";
        assertEquals(message, notification2.getPayload(), receivedMessage.getPayload());
    }
}
