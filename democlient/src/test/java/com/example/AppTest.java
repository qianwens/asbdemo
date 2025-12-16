package com.example;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }
    
    /**
     * Test that the queue name constant is properly defined
     */
    @Test
    public void testQueueNameConstantExists()
    {
        // Use reflection to verify QUEUE_NAME constant exists
        try {
            java.lang.reflect.Field field = App.class.getDeclaredField("QUEUE_NAME");
            assertNotNull("QUEUE_NAME field should exist", field);
            field.setAccessible(true);
            String queueName = (String) field.get(null);
            assertTrue("QUEUE_NAME should be 'news'", "news".equals(queueName));
        } catch (Exception e) {
            throw new AssertionError("QUEUE_NAME constant should be defined", e);
        }
    }
}
