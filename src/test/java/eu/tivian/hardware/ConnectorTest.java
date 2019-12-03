package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorTest {
    @Test
    void logic() {
        Connector male   = new Connector("test male"  , Connector.Gender.MALE  );
        Connector female = new Connector("test female", Connector.Gender.FEMALE);

        female.connect(male);

        for (int i = 0; i < 5; i++) {
            male.add(  new Pin(Pin.Direction.INPUT ));
            female.add(new Pin(Pin.Direction.OUTPUT));
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(Pin.Level.LOW , male.get(i).level());
            female.get(i).level(Pin.Level.HIGH);
            assertEquals(Pin.Level.HIGH, male.get(i).level());
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(Pin.Level.HIGH, male.get(i).level());
            female.get(i).level(Pin.Level.LOW);
            assertEquals(Pin.Level.LOW , male.get(i).level());
        }

        male.remove(0);
        assertEquals(4, male.size());
        assertEquals(4, female.size());

        female.get(0).level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, male.get(0).level());
        male.disconnect();
        assertEquals(Pin.Level.LOW , male.get(0).level());
    }
}