package org.wahlzeit.model;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.wahlzeit.handlers.LocalDatastoreServiceTestConfigProvider;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link Guest}.
 * <p/>
 * Created by Lukas Hahmann on 29.05.15.
 */
public class GuestTest {

    @ClassRule
    public static LocalDatastoreServiceTestConfigProvider localDatastoreServiceTestConfigProvider =
            new LocalDatastoreServiceTestConfigProvider();

    @BeforeClass
    public static void setClassUp() {
        ObjectifyService.factory().register(Client.class);
    }

    @Test
    public void testNameGeneration() {
        assertNewGuestHasId(1);
        assertNewGuestHasId(2);
        // creation of user should not consume a next id
        ObjectifyService.run(new Work<Void>() {
            @Override
            public Void run() {
                new User("han", "solo", "star@wa.rs", 1337L);
                return null;
            }
        });
        assertNewGuestHasId(3);
    }

    protected void assertNewGuestHasId(int id) {
        Guest testGuest = new Guest();
        String userName = testGuest.getName();
        String expectedUserName = Guest.GUEST_PREFIX + id;
        assertEquals(expectedUserName, userName);
    }
}
