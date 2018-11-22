package org.tango.rest.test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import fr.esrf.Tango.ErrSeverity;
import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeBlobBuilder;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tango.rest.rc5.ClientHelper;
import org.tango.rest.rc5.entities.*;
import org.tango.rest.rc5.entities.pipe.Pipe;
import org.tango.rest.rc5.entities.pipe.PipeValue;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.12.2015
 */
public class Rc5Test {
    public static final String REST_API_VERSION = "rc5";
    private static Context CONTEXT;
    private Client client;

    @BeforeClass
    public static void beforeClass(){
        CONTEXT = Context.create(REST_API_VERSION);
    }

    @Before
    public void before(){
        client = ClientHelper.initializeClientWithBasicAuthentication(CONTEXT.url, CONTEXT.user, CONTEXT.password);
    }

    @Test
    public void testVersion(){
        Map<String,String> result = client.target(CONTEXT.url).request().get(HashMap.class);

        Assert.assertTrue(result.containsKey(REST_API_VERSION));
    }

    @Test
    public void testHost(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("hosts/localhost;port=10000");
        TangoHost result = client.target(uriBuilder.build()).request().get(TangoHost.class);

        Assert.assertEquals("localhost:10000", result.id);
        Assert.assertEquals("localhost", result.host);
        Assert.assertEquals("10000", result.port);
        Assert.assertEquals("sys/database/2", result.name);
    }

    @Test(expected = BadRequestException.class)
    public void testHost_wrongPort(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("hosts/localhost;port=12345");
        TangoHost result = client.target(uriBuilder.build()).request().get(TangoHost.class);

        Assert.fail();
    }

    @Test(expected = NotFoundException.class)
    public void testHost_nonExistingHost(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("hosts/XXXX");
        TangoHost result = client.target(uriBuilder.build()).request().get(TangoHost.class);

        Assert.fail();
    }

    @Test
    public void testTangoHostDevicesWith(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.devicesUri);
        List<NamedEntity> result = client.target(uriBuilder.build()).request().get(new GenericType<List<NamedEntity>>(){});

        Assert.assertFalse(result.isEmpty());
        NamedEntity entity = Iterables.find(result, new Predicate<NamedEntity>() {
            @Override
            public boolean apply(@Nullable NamedEntity input) {
                return input.name.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(entity);
    }

    @Test
    public void testTangoHostDevicesWithWildCard(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.devicesUri).queryParam("wildcard","sys/tg_test/1");
        List<NamedEntity> result = client.target(uriBuilder.build()).request().get(new GenericType<List<NamedEntity>>(){});

        Assert.assertFalse(result.isEmpty());
        NamedEntity entity = Iterables.find(result, new Predicate<NamedEntity>() {
            @Override
            public boolean apply(@Nullable NamedEntity input) {
                return input.name.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(entity);
    }

    @Test
    public void testTangoTestIsPresent(){
        List<NamedEntity> result = client.target(CONTEXT.devicesUri).request().get(new GenericType<List<NamedEntity>>() {
        });

        Assert.assertTrue(Iterables.tryFind(result, new Predicate<NamedEntity>() {
            @Override
            public boolean apply(NamedEntity input) {
                return input.name.equals(CONTEXT.SYS_TG_TEST_1);
            }
        }).isPresent());
    }

    @Test(expected = NotFoundException.class)
    public void testTangoDeviceNotDefinedInDb(){
        Device result = client.target(UriBuilder.fromUri(CONTEXT.devicesUri).path("X/Y/Z").build()).request().get(Device.class);

        Assert.fail();
    }

    @Test
    public void testDevicesTree(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("devices/tree").queryParam("host","localhost").queryParam("wildcard","sys/tg_test/1");
        List<org.tango.rest.rc5.tree.TangoHost> result = client.target(uriBuilder.build()).request().get(new GenericType<List<org.tango.rest.rc5.tree.TangoHost>>(){});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.get(0).isAlive);
        Assert.assertEquals("aliases",result.get(0).data.get(0).value);
        Assert.assertEquals("sys",result.get(0).data.get(1).value);
    }

    @Test
    public void testDevicesTreeForLocalhost(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path("tree");
        List<org.tango.rest.rc5.tree.TangoHost> result = client.target(uriBuilder.build()).request().get(new GenericType<List<org.tango.rest.rc5.tree.TangoHost>>(){});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.get(0).isAlive);
        Assert.assertEquals("aliases",result.get(0).data.get(0).value);
        Assert.assertEquals("sys",result.get(0).data.get(1).value);
    }

    //TODO tree -- wrong Tango host e.g. port, host

    @Test
    public void testAttributes(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("attributes").queryParam("wildcard", "localhost:10000/*/*/*/State");
        List<Attribute> result = client.target(uriBuilder.build()).request().get(new GenericType<List<Attribute>>(){});

        Attribute attribute = Iterables.find(result, new Predicate<Attribute>() {
            @Override
            public boolean apply(@Nullable Attribute input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(attribute);
    }

    @Test
    public void testAttributeValuesRead(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("attributes/value").queryParam("wildcard", "localhost:10000/*/*/*/State");
        List<AttributeValue> result = client.target(uriBuilder.build()).request().get(new GenericType<List<AttributeValue>>(){});

        AttributeValue attribute = Iterables.find(result, new Predicate<AttributeValue>() {
            @Override
            public boolean apply(@Nullable AttributeValue input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertEquals("RUNNING", attribute.value);
    }

    @Test
    public void testAttributeValuesWrite(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("attributes/value");
        List<AttributeValue> result = client.target(uriBuilder.build()).request().put(
                Entity.entity(Lists.<AttributeValue>newArrayList(
                    new AttributeValue<>("double_scalar_w","localhost:10000","sys/tg_test/1",3.14D,null,0L)
                ),MediaType.APPLICATION_JSON_TYPE),
                new GenericType<List<AttributeValue>>(){});

        AttributeValue attribute = Iterables.find(result, new Predicate<AttributeValue>() {
            @Override
            public boolean apply(@Nullable AttributeValue input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertEquals(3.14, attribute.value);
    }

    @Test
    public void testAttributeValuesWrite_wrongValueType(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("attributes/value");
        List<AttributeValue> result = client.target(uriBuilder.build()).request().put(
                Entity.entity(Lists.<AttributeValue>newArrayList(
                        new AttributeValue<>("double_scalar_w","localhost:10000","sys/tg_test/1","Hello World!",null,0L)
                ),MediaType.APPLICATION_JSON_TYPE),
                new GenericType<List<AttributeValue>>(){});

        AttributeValue attribute = Iterables.find(result, new Predicate<AttributeValue>() {
            @Override
            public boolean apply(@Nullable AttributeValue input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertSame(ErrSeverity.PANIC, attribute.errors[0].severity);
    }


    @Test
    public void testCommands(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("commands").queryParam("wildcard", "localhost:10000/*/*/*/init");
        List<Command> result = client.target(uriBuilder.build()).request().get(new GenericType<List<Command>>(){});

        Command command = Iterables.find(result, new Predicate<Command>() {
            @Override
            public boolean apply(@Nullable Command input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(command);
        Assert.assertEquals("localhost:10000/sys/tg_test/1/Init", command.id);
        Assert.assertEquals("Init", command.name);
        Assert.assertEquals("sys/tg_test/1", command.device);
        Assert.assertEquals("localhost:10000", command.host);
        Assert.assertEquals("OPERATOR", command.info.level);
    }

    @Test
    public void testCommands_execute(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("commands");
        List<CommandInOut<?,?>> result = client.target(uriBuilder.build()).request().put(
                Entity.entity(Lists.<CommandInOut<?,?>>newArrayList(
                        new CommandInOut<Double,Double>("localhost:10000","sys/tg_test/1","DevDouble",3.14D),
                        new CommandInOut<String,String>("localhost:10000","sys/tg_test/1","DevString","Hello World!")
                ),MediaType.APPLICATION_JSON_TYPE)
                ,new GenericType<List<CommandInOut<?,?>>>(){});

        CommandInOut<?, ?> command = Iterables.find(result, new Predicate<CommandInOut<?, ?>>() {
            @Override
            public boolean apply(@Nullable CommandInOut<?, ?> input) {
                return input.name.equalsIgnoreCase("DevString");
            }
        });

        Assert.assertNotNull(command);
        Assert.assertEquals("Hello World!", command.output);

        command = Iterables.find(result, new Predicate<CommandInOut<?, ?>>() {
            @Override
            public boolean apply(@Nullable CommandInOut<?, ?> input) {
                return input.name.equalsIgnoreCase("DevDouble");
            }
        });

        Assert.assertNotNull(command);
        Assert.assertEquals(3.14D, command.output);
    }

    @Test
    public void testPipes(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("pipes").queryParam("wildcard", CONTEXT.tango_host + ":" +CONTEXT.tango_port + "/*/*/*/string_long_short_ro");
        List<Pipe> result = client.target(uriBuilder.build()).request().get(new GenericType<List<Pipe>>(){});

        Pipe pipe = Iterables.find(result, new Predicate<Pipe>() {
            @Override
            public boolean apply(@Nullable Pipe input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(pipe);
    }

    @Test
    public void testPipesValue(){
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.uri).path("pipes/value").queryParam("wildcard", CONTEXT.tango_host + ":" +CONTEXT.tango_port + "/*/*/*/string_long_short_ro");
        List<PipeValue> result = client.target(uriBuilder.build()).request().get(new GenericType<List<PipeValue>>(){});

        PipeValue pipe = Iterables.find(result, new Predicate<PipeValue>() {
            @Override
            public boolean apply(@Nullable PipeValue input) {
                return input.device.equalsIgnoreCase("sys/tg_test/1");
            }
        });

        Assert.assertNotNull(pipe);
    }



    @Test
    public void testAttribute(){
        //again if this one does not fail test passes
        Attribute attribute = client.target(CONTEXT.longScalarWUri)
                .request().get(Attribute.class);

        Assert.assertNotNull(attribute);
        Assert.assertEquals("localhost:10000/sys/tg_test/1/long_scalar_w", attribute.id);
        Assert.assertEquals("localhost:10000", attribute.host);
        Assert.assertEquals("sys/tg_test/1", attribute.device);
        Assert.assertEquals("long_scalar_w", attribute.name);
        Assert.assertEquals("long_scalar_w", attribute.info.name);
    }

    @Test(expected = NotFoundException.class)
    public void testAttribute_notFound(){
        //again if this one does not fail test passes
        Attribute attribute = client.target(UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1)).path("attributes/XXXX")
                .request().get(Attribute.class);

        Assert.fail();
    }


    @Test
    public void testTangoTestInfo() {
        //if it does not fail with deserialization exception response confronts API spec
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1);
        URI uri = uriBuilder.build();
        Device result = client.target(uri).request().get(Device.class);

        //just make sure we have all we need for further tests
        Assert.assertEquals("sys/tg_test/1", result.name);
        Assert.assertEquals(new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").build().toString(), result.attributes);
        Assert.assertEquals(new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("commands").build().toString(), result.commands);
        Assert.assertEquals(new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes").build().toString(), result.pipes);
        Assert.assertEquals(new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("properties").build().toString(), result.properties);
        Assert.assertEquals(new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("state").build().toString(), result.state);

        DeviceInfo info = result.info;
        Assert.assertNotNull(info.ior);
        Assert.assertFalse(info.is_taco);
        Assert.assertTrue(info.exported);
        Assert.assertNotNull(info.last_exported);
        Assert.assertNotNull(info.last_unexported);
        Assert.assertEquals("sys/tg_test/1", info.name);
        Assert.assertEquals("unknown", info.classname);
        Assert.assertNotNull(info.version);
        Assert.assertEquals("TangoTest/test", info.server);
        Assert.assertNotNull(info.hostname);
    }

    @Test
    public void testTangoTestAttributes() {
        //if it does not fail with deserialization exception response confronts API spec
        UriBuilder uriBuilder = new ResteasyUriBuilder().uri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes");
        URI uri = uriBuilder.build();
        List<Attribute> result = client.target(uri).request().get(new GenericType<List<Attribute>>(){});

        Attribute attribute = Iterables.find(result, new Predicate<Attribute>() {
            @Override
            public boolean apply(@Nullable Attribute input) {
                return input.name.equalsIgnoreCase("double_scalar");
            }
        });

        Assert.assertNotNull(attribute);
    }

    @Test
    public void testWriteReadAttribute(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).path("value").queryParam("v", "123456").build();
        AttributeValue<Integer> result = client.target(uri)
                .request().put(null, new GenericType<AttributeValue<Integer>>() {
                });

        Assert.assertEquals(123456, result.value.intValue());
    }

    @Test
    public void testAttributeValuePlain(){
        //again if this one does not fail test passes
        int value = client.target(CONTEXT.longScalarWUri).path("value")

                .request().header("Accept", MediaType.TEXT_PLAIN).get(int.class);

        Assert.assertEquals(123456, value);
    }

    @Test
    public void testAttributeValueImage(){
        //again if this one does not fail test passes
        String value = client.target(CONTEXT.uShortImageRO).path("value")

                .request().header("Accept", "image/jpeg").get(String.class);

        Assert.assertTrue(value.startsWith("data:/jpeg;base64"));
    }

    @Test(expected = javax.ws.rs.BadRequestException.class)
    public void testAttributeValueImage_nonImageAttribute(){
        //again if this one does not fail test passes
        String value = client.target(CONTEXT.longScalarWUri).path("value")

                .request().header("Accept", "image/jpeg").get(String.class);

        Assert.fail();
    }

    @Test
    public void testWriteAttributeAsync(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).path("value").queryParam("v", 123456).queryParam("async", true).build();
        AttributeValue<Integer> result = client.target(uri)
                .request().put(null, new GenericType<AttributeValue<Integer>>() {
                });

        Assert.assertNull(result);
    }

    @Test
    public void testWriteReadSpectrum(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").path("double_spectrum").path("value").queryParam("v", "3.14,2.87,1.44").build();//TODO native array does not work

        AttributeValue<double[]> result = client.target(uri)
                .request().put(null, new GenericType<AttributeValue<double[]>>() {
                });

        Assert.assertArrayEquals(new double[]{3.14,2.87,1.44},result.value, 0.0);
    }

    @Test(expected = NotFoundException.class)
    public void testWriteAttribute_doesNotExists(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").path("XXX").path("value").queryParam("v", "3.14,2.87,1.44").build();//TODO native array does not work

        AttributeValue<double[]> result = client.target(uri)
                .request().put(null, new GenericType<AttributeValue<double[]>>() {
                });

        Assert.fail();
    }

    @Test
    public void testCommand(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("commands").path("DevString").build();

        //if parsed w/o exception consider test has passed
        Command cmd = client.target(uri)
                .request().get(Command.class);


        Assert.assertEquals("localhost:10000/sys/tg_test/1/DevString", cmd.id);
        Assert.assertEquals("DevString", cmd.name);
        Assert.assertEquals("sys/tg_test/1", cmd.device);
        Assert.assertEquals("localhost:10000", cmd.host);
        Assert.assertEquals("OPERATOR", cmd.info.level);
    }

    @Test(expected = NotFoundException.class)
    public void testCommand_NotFound(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("commands").path("XXXX").build();

        //if parsed w/o exception consider test has passed
        Command cmd = client.target(uri)
                .request().get(Command.class);


        Assert.fail();
    }

    @Test
    public void testExecuteCommand(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("commands").path("DevString").build();

        CommandInOut<String, String> input = new CommandInOut<>();
        input.input = "Hello World!!!";

        CommandInOut<String,String> result = client.target(uri)
                .request()
//                .header("Accept", MediaType.APPLICATION_JSON)
                .put(
                        Entity.entity(input, MediaType.APPLICATION_JSON_TYPE),
                        new GenericType<CommandInOut<String, String>>() {
                });

        Assert.assertEquals("Hello World!!!", result.output);
    }

//    @Test
    public void testExecuteCommand_AcceptPlain(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("commands").path("DevString").build();

        CommandInOut<String, String> input = new CommandInOut<>();
        input.input = "Hello World!!!";

        String result = client.target(uri)
                .request()
                .header("Accept", MediaType.TEXT_PLAIN)
                .put(
                        Entity.entity(input, MediaType.APPLICATION_JSON_TYPE),
                        String.class);

        Assert.assertEquals("Hello World!!!", result);
    }

    //TODO properties

    @Test
    public void testDevicePipes(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes").build();

        List<NamedEntity> result = client.target(uri)
                .request()
//                .header("Accept", MediaType.APPLICATION_JSON)
                .get(
                        new GenericType<List<NamedEntity>>() {
                        });

        NamedEntity pipe = Iterables.find(result, new Predicate<NamedEntity>() {
            @Override
            public boolean apply(@Nullable NamedEntity input) {
                return input.name.equalsIgnoreCase("string_long_short_ro");
            }
        });

        Assert.assertNotNull(pipe);
    }

    @Test
    public void testDevicePipe(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes/string_long_short_ro").build();

        Pipe result = client.target(uri)
                .request()
//                .header("Accept", MediaType.APPLICATION_JSON)
                .get(Pipe.class);

        Assert.assertNotNull(result);
    }

    @Test
    public void testDevicePipeValueRead(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes/string_long_short_ro/value").build();

        PipeValue result = client.target(uri)
                .request()
//                .header("Accept", MediaType.APPLICATION_JSON)
                .get(PipeValue.class);

        Assert.assertNotNull(result);
        Assert.assertEquals(CONTEXT.tango_host + ":" + CONTEXT.tango_port, result.host);
        Assert.assertEquals("sys/tg_test/1", result.device);
        Assert.assertEquals("string_long_short_ro", result.name);
        Assert.assertNotNull(result.data);
        Assert.assertEquals("FirstDE", result.data.get(0).name);
        Assert.assertArrayEquals(new String[]{"The string"}, result.data.get(0).value.toArray());
        Assert.assertEquals("SecondDE", result.data.get(1).name);
        Assert.assertArrayEquals(new int[]{666}, Ints.toArray((List<Integer>)result.data.get(1).value));
        Assert.assertEquals("ThirdDE", result.data.get(2).name);
        Assert.assertArrayEquals(new int[]{12}, Ints.toArray((List<Integer>)result.data.get(2).value));
    }

    //TODO writable Pipe in TangoTest
    @Test(expected = BadRequestException.class)
    public void testDevicePipeValueWrite(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes/string_long_short_ro/value").build();

        PipeValue result = client.target(uri)
                .request()
//                .header("Accept", MediaType.APPLICATION_JSON)
                .put(Entity.entity(
                        new PipeBlobBuilder("blob1").add("FirstDE", "Hello World!").build()
                        ,MediaType.APPLICATION_JSON),PipeValue.class);

        Assert.assertNotNull(result);
    }


    //TODO events

    @Test
    public void testPartitioning(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").queryParam("range", "5-10").build();

        List<Attribute> result = client.target(uri).request().get(new GenericType<List<Attribute>>() {
        });

        Assert.assertTrue(result.size() == 5);
    }

    @Test(expected = ClientErrorException.class)
    public void testPartitioning_wrongRange(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").queryParam("range", "XXX").build();

        List<Attribute> result = client.target(uri).request().get(new GenericType<List<Attribute>>() {
        });

        Assert.fail();
    }

    @Test
    public void testFilteringAttribute(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).queryParam("filter", "name").build();

        Attribute result = client.target(uri)
                .request().get(Attribute.class);


        Assert.assertNotNull(result);
        Assert.assertEquals("long_scalar_w", result.name);
        Assert.assertNull(result.value);
        Assert.assertNotNull(result.info);
        Assert.assertEquals("long_scalar_w", result.info.name);
        Assert.assertNull(result.info.label);
    }

    @Test
    public void testFilteringPipeValue(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("pipes/string_long_short_ro/value").queryParam("filter","name").build();

        PipeValue result = client.target(uri)
                .request().get(PipeValue.class);


        Assert.assertNotNull(result);
        Assert.assertEquals("string_long_short_ro", result.name);
        Assert.assertNull(result.data);
    }

    @Test
    public void testFilteringInverted(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).queryParam("filter", "!name").build();

        Attribute result = client.target(uri)
                .request().get(Attribute.class);

        Assert.assertNull(result.name);
        Assert.assertNotNull(result.value);
    }

    @Test(expected = BadRequestException.class)
    public void testNoValue(){
        URI uri = UriBuilder.fromUri(CONTEXT.devicesUri).path(CONTEXT.SYS_TG_TEST_1).path("attributes").path("no_value").path("value").build();

        AttributeValue<?> result = client.target(uri)
                .request().get(new GenericType<AttributeValue<?>>() {
                });
    }

    @Test
    public void testAttributeInfo(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).path("info").build();

        AttributeInfo result = client.target(uri)
                .request().get(AttributeInfo.class);

        Assert.assertNotNull(result);
        Assert.assertEquals("long_scalar_w", result.name);
        Assert.assertEquals("WRITE", result.writable);
        Assert.assertEquals("SCALAR", result.data_format);
        Assert.assertEquals("OPERATOR", result.level);
    }

    @Test
    public void testAttributeInfoPut(){
        URI uri = UriBuilder.fromUri(CONTEXT.longScalarWUri).path("info").build();

        AttributeInfo info = client.target(uri)
                .request().get(AttributeInfo.class);

        info.alarms.max_alarm = "1000";
        info.events.ch_event.rel_change = "100";

        AttributeInfo result = client.target(uri)
                .request().put(Entity.entity(info, MediaType.APPLICATION_JSON_TYPE), AttributeInfo.class);

        Assert.assertEquals("1000", result.alarms.max_alarm);
        Assert.assertEquals("100", result.events.ch_event.rel_change);
    }
}
