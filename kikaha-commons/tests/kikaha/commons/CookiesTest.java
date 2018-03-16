package kikaha.commons;

import io.undertow.util.Rfc6265CookieSupport;
import kikaha.commons.Cookie;
import kikaha.commons.Cookies;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

public class CookiesTest {

    @Test
    public void testParsingSetCookieHeaderV0() {

        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_E_COYOTE; path=/; expires=Friday, 16-Mar-18 23:12:40 GMT");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        Assert.assertEquals("/", cookie.getPath());
        Assert.assertEquals(date(2018, 3, 16, 23, 12, 40), cookie.getExpires());


        cookie = Cookies.parseSetCookieHeader("SHIPPING=FEDEX; path=/foo; secure");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("/foo", cookie.getPath());
        Assert.assertTrue(cookie.isSecure());

        cookie = Cookies.parseSetCookieHeader("SHIPPING=FEDEX");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
    }

    @Test
    public void testParsingSetCookieHeaderV1() {
        Cookie cookie = Cookies.parseSetCookieHeader("Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\"");
        Assert.assertEquals("Customer", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        Assert.assertEquals("/acme", cookie.getPath());
        Assert.assertEquals(1, cookie.getVersion());


        cookie = Cookies.parseSetCookieHeader("SHIPPING=\"FEDEX\"; path=\"/foo\"; secure; Version=\"1\";");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("/foo", cookie.getPath());
        Assert.assertTrue(cookie.isSecure());
        Assert.assertEquals(1, cookie.getVersion());
    }

    private static Date date(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
        c.set(year, month-1, day, hour, minute, second);
        return c.getTime();
    }

    @Test
    public void testInvalidCookie() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(1, false, Arrays.asList("\"; CUSTOMER=WILE_E_COYOTE"));

        Assert.assertFalse(cookies.containsKey("$Domain"));
        Assert.assertFalse(cookies.containsKey("$Version"));
        Assert.assertFalse(cookies.containsKey("$Path"));

        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());

        cookies = Cookies.parseRequestCookies(1, false, Arrays.asList("; CUSTOMER=WILE_E_COYOTE"));

        cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());

        cookies = Cookies.parseRequestCookies(1, false, Arrays.asList("foobar; CUSTOMER=WILE_E_COYOTE"));

        cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
    }
    @Test
    public void testRequestCookieDomainPathVersion() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(1, false, Arrays.asList(
                "CUSTOMER=WILE_E_COYOTE; $Domain=LOONEY_TUNES; $Version=1; $Path=/"));

        Assert.assertFalse(cookies.containsKey("$Domain"));
        Assert.assertFalse(cookies.containsKey("$Version"));
        Assert.assertFalse(cookies.containsKey("$Path"));

        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());
    }

    @Test
    public void testMultipleRequestCookies() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, false, Arrays.asList(
                "CUSTOMER=WILE_E_COYOTE; $Domain=LOONEY_TUNES; $Version=1; $Path=/; SHIPPING=FEDEX"));

        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());

        cookie = cookies.get("SHIPPING");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());
    }

    @Test
    public void testEqualsInValueNotAllowed() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, false, Arrays.asList("CUSTOMER=WILE_E_COYOTE=THE_COYOTE; SHIPPING=FEDEX"));
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());
    }

    @Test
    public void testEmptyCookieNames() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(4, false, Arrays.asList("=foo; CUSTOMER=WILE_E_COYOTE=THE_COYOTE; =foobar; SHIPPING=FEDEX; =bar"));
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());
        cookie = cookies.get("");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("foo", cookie.getValue());
    }

    @Test
    public void testEqualsInValueAllowed() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(1, true, Arrays.asList("CUSTOMER=WILE_E_COYOTE=THE_COYOTE"));
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE=THE_COYOTE", cookie.getValue());
    }

    @Test
    public void testEqualsInValueAllowedInQuotedValue() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, true, Arrays.asList("CUSTOMER=\"WILE_E_COYOTE=THE_COYOTE\"; SHIPPING=FEDEX" ));
        Assert.assertEquals(2, cookies.size());
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE=THE_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());
    }

    @Test
    public void testEqualsInValueNotAllowedInQuotedValue() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, false, Arrays.asList("CUSTOMER=\"WILE_E_COYOTE=THE_COYOTE\"; SHIPPING=FEDEX" ));
        Assert.assertEquals(2, cookies.size());
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE=THE_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());
    }
    @Test
    public void testCommaSeparatedCookies() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, false, Arrays.asList("CUSTOMER=\"WILE_E_COYOTE\", SHIPPING=FEDEX" ), true);
        Assert.assertEquals(2, cookies.size());
        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());

        //also make sure semi colon works as normal
        cookies = Cookies.parseRequestCookies(2, false, Arrays.asList("CUSTOMER=\"WILE_E_COYOTE\"; SHIPPING=FEDEX" ), true);
        Assert.assertEquals(2, cookies.size());
        cookie = cookies.get("CUSTOMER");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        cookie = cookies.get("SHIPPING");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("FEDEX", cookie.getValue());
    }

    @Test
    public void testSimpleJSONObjectInRequestCookies() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, true, Arrays.asList(
                "CUSTOMER={\"v1\":1, \"id\":\"some_unique_id\", \"c\":\"http://www.google.com?q=love me\"};"
                        + " $Domain=LOONEY_TUNES; $Version=1; $Path=/; SHIPPING=FEDEX"));

        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("{\"v1\":1, \"id\":\"some_unique_id\", \"c\":\"http://www.google.com?q=love me\"}",
                cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());

        cookie = cookies.get("SHIPPING");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());
    }

    @Test
    public void testComplexJSONObjectInRequestCookies() {
        Map<String, Cookie> cookies = Cookies.parseRequestCookies(2, false, Arrays.asList(
                "CUSTOMER={ \"accounting\" : [ { \"firstName\" : \"John\", \"lastName\" : \"Doe\", \"age\" : 23 },"
                        + " { \"firstName\" : \"Mary\",  \"lastName\" : \"Smith\", \"age\" : 32 }], "
                        + "\"sales\" : [ { \"firstName\" : \"Sally\", \"lastName\" : \"Green\", \"age\" : 27 }, "
                        + "{ \"firstName\" : \"Jim\", \"lastName\" : \"Galley\", \"age\" : 41 } ] };"
                        + " $Domain=LOONEY_TUNES; $Version=1; $Path=/; SHIPPING=FEDEX"));

        Cookie cookie = cookies.get("CUSTOMER");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("{ \"accounting\" : [ { \"firstName\" : \"John\", \"lastName\" : \"Doe\", \"age\" : 23 },"
                        + " { \"firstName\" : \"Mary\",  \"lastName\" : \"Smith\", \"age\" : 32 }], "
                        + "\"sales\" : [ { \"firstName\" : \"Sally\", \"lastName\" : \"Green\", \"age\" : 27 }, "
                        + "{ \"firstName\" : \"Jim\", \"lastName\" : \"Galley\", \"age\" : 41 } ] }",
                cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());

        cookie = cookies.get("SHIPPING");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("LOONEY_TUNES", cookie.getDomain());
        Assert.assertEquals(1, cookie.getVersion());
        Assert.assertEquals("/", cookie.getPath());
    }

    @Test @Ignore
    public void testSameSiteCookie() {
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_E_COYOTE; path=/; SameSite");
        Assert.assertEquals("CUSTOMER", cookie.getName());
        Assert.assertEquals("WILE_E_COYOTE", cookie.getValue());
        Assert.assertEquals("/", cookie.getPath());
        /*Assert.assertTrue(cookie.isSameSite());
        Assert.assertNull(cookie.getSameSiteMode());*/

        cookie = Cookies.parseSetCookieHeader("SHIPPING=FEDEX; path=/foo; SameSite=Strict");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("/foo", cookie.getPath());
        /*Assert.assertTrue(cookie.isSameSite());
        Assert.assertEquals("Strict", cookie.getSameSiteMode());*/

        cookie = Cookies.parseSetCookieHeader("SHIPPING=FEDEX; path=/acme; SameSite=Lax");
        Assert.assertEquals("SHIPPING", cookie.getName());
        Assert.assertEquals("FEDEX", cookie.getValue());
        Assert.assertEquals("/acme", cookie.getPath());
        /*Assert.assertTrue(cookie.isSameSite());
        Assert.assertEquals("Lax", cookie.getSameSiteMode());*/
    }

    @Test(expected = IllegalArgumentException.class) @Ignore
    public void testInvalidSameSiteCookie() {
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_E_COYOTE; path=/; SameSite=test");
    }

    // RFC6265 allows US-ASCII characters excluding CTLs, whitespace,
    // double quote, comma, semicolon and backslash as cookie value.
    // This does not change even if value is quoted.
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue() {
        // whitespace is not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_ E_COYOTE; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue1() {
        // whitespace is not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=\"WILE_ E_COYOTE\"; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue2() {
        // double quote si not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=\"WILE_\\\"E_COYOTE\"; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue3() {
        // comma is not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=\"WILE_,E_COYOTE\"; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue4() {
        // semicolon is not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=\"WILE_;E_COYOTE\"; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInValue5() {
        /// backslash is not allowed
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=\"WILE_\\E_COYOTE\"; path=/example; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
    }

    // RFC6265 allows any CHAR except CTLs or ";" as cookie path
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInPath() {
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_E_COYOTE; path=\"/ex;ample\"; domain=example.com");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
        Rfc6265CookieSupport.validatePath(cookie.getPath());
        Rfc6265CookieSupport.validateDomain(cookie.getDomain());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRfc6265CookieInDomain() {
        Cookie cookie = Cookies.parseSetCookieHeader("CUSTOMER=WILE_E_COYOTE; path=/example; domain=\"ex;ample.com\"");
        Rfc6265CookieSupport.validateCookieValue(cookie.getValue());
        Rfc6265CookieSupport.validatePath(cookie.getPath());
        Rfc6265CookieSupport.validateDomain(cookie.getDomain());
    }

}
