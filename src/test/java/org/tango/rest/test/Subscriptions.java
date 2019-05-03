package org.tango.rest.test;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tango.rest.test.v10.ClientHelper;
import org.tango.rest.v10.event.Event;
import org.tango.rest.v10.event.Subscription;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.tango.rest.test.V10Test.REST_API_VERSION;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 5/3/19
 */
public class Subscriptions {
    private static Context CONTEXT;
    private Client client;
    private URI uri = null;

    @BeforeClass
    public static void beforeClass() {
        CONTEXT = Context.create(REST_API_VERSION);
    }

    @Before
    public void before() throws Exception {
        //since Tango REST API v1.1 subscriptions is a dedicated entry point
        URL url = new URL(System.getProperty("tango.rest.url"));
        uri = url.toURI().resolve("./subscriptions");


        client = ClientHelper.initializeClientWithBasicAuthentication(uri.toString(), CONTEXT.user, CONTEXT.password);
    }


    private void createSubscription() {
        Subscription result = client.target(uri)
                .request().post(null, Subscription.class);

        assertNotNull(result);
        assertEquals(1, result.id);
    }

    @Test
    public void testSubscriptions() throws Exception {
        createSubscription();

        subscriptionAddEvents();

        subscriptionAddNonExistingAttribute();

        testEventStream();
    }

    public void subscriptionAddEvents() {
        URI subscription = UriBuilder.fromUri(this.uri).path("1").build();


        List<Event> result = client.target(subscription)
                .request()
                .put(
                        Entity.entity(
                                Lists.newArrayList(
                                        new Event.Target("localhost:10000", "sys/tg_test/1", "double_scalar", "change"),
                                        new Event.Target("localhost:10000", "sys/tg_test/1", "long_scalar", "periodic")
                                ),
                                MediaType.APPLICATION_JSON_TYPE),
                        new GenericType<List<Event>>() {
                        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).id);
        assertEquals("localhost:10000", result.get(0).target.host);
        assertEquals("sys/tg_test/1", result.get(0).target.device);
        assertEquals("double_scalar", result.get(0).target.attribute);
        assertEquals("change", result.get(0).target.type);
        assertEquals(2, result.get(1).id);
        assertEquals("localhost:10000", result.get(1).target.host);
        assertEquals("sys/tg_test/1", result.get(1).target.device);
        assertEquals("long_scalar", result.get(1).target.attribute);
        assertEquals("periodic", result.get(1).target.type);
    }

    //TODO requires Tango event subscription logic change
//    @Test/*(expected = BadRequestException.class)*/
    public void subscriptionAddNonExistingAttribute() {
        URI subscription = UriBuilder.fromUri(this.uri).path("1").build();


        List<Event> result = client.target(subscription)
                .request()
                .put(
                        Entity.entity(
                                Lists.newArrayList(
                                        new Event.Target("localhost:10000", "sys/tg_test/1", "XXXX", "change")
                                ),
                                MediaType.APPLICATION_JSON_TYPE),
                        new GenericType<List<Event>>() {
                        });

        assertNotNull(result);
    }

    public void testEventStream() throws Exception {
        URI subscription = UriBuilder.fromUri(this.uri).path("1/event-stream").build();

        CompletableFuture<String> future = new CompletableFuture<>();
        WebTarget target = client.target(subscription);
        SseEventSource eventSource = SseEventSource.target(target).build();
        eventSource.register(event -> {
            String data = event.readData();
            System.out.println("Received an event: " + data);
            future.complete(data);
        }, ex -> {
            System.err.println("Received an exception: " + ex.getMessage());
            future.completeExceptionally(ex);
        });
        eventSource.open();
        assertNotNull(future.get());
    }
}
