package com.url.shortener.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Base62EncoderTest {

    @Test
    void generateBase62Encoder() {
        String rawKey = Base62Encoder.encode(0);
        assertEquals(rawKey,"0");
    }
    @Test
    void generateBase62EncoderForSmall(){
        String rawKey=Base62Encoder.encode(1);
        assertEquals(rawKey,"1");
    }

    @Test
    void encode_shouldProduceDifferentOutputsForDifferentInputs() {
        String key1 = Base62Encoder.encode(4359874);
        String key2 = Base62Encoder.encode(9876);
        assertNotEquals(key1, key2);
    }
    @Test
    void generateSameCodeForSameInput() {
        String key1 = Base62Encoder.encode(1);
        String key2 = Base62Encoder.encode(1);
        assertEquals(key2, key1);
    }
}
