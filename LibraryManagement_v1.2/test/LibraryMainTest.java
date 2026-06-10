import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class LibraryMainTest {

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUpStreams() {
        originalIn = System.in;
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() throws Exception {
        System.setIn(originalIn);
        System.setOut(originalOut);
        setStaticField("manager", null);
        setStaticField("sc", new Scanner(System.in));
    }

    @Test
    @DisplayName("정상 ID/PW 입력 시 로그인 성공")
    void performLoginReturnsTrueWhenManagerLoginSucceeds() throws Exception {
        FakeLibraryManager fakeManager = new FakeLibraryManager();
        fakeManager.acceptedId = "admin";
        fakeManager.acceptedPassword = "1111";

        setInput("admin\n1111\n");
        setStaticField("manager", fakeManager);

        boolean result = invokePerformLogin();

        assertTrue(result);
        assertEquals(1, fakeManager.loginCallCount);
        assertEquals("admin", fakeManager.lastId);
        assertEquals("1111", fakeManager.lastPassword);

        printPass("정상 ID/PW 입력 시 로그인 성공 테스트 통과");
    }

    @Test
    @DisplayName("숫자로 시작하는 ID 입력 시 재입력 후 로그인 성공")
    void performLoginRejectsIdStartingWithDigitAndRetries() throws Exception {
        FakeLibraryManager fakeManager = new FakeLibraryManager();
        fakeManager.acceptedId = "admin";
        fakeManager.acceptedPassword = "1111";

        setInput("1admin\nadmin\n1111\n");
        setStaticField("manager", fakeManager);

        boolean result = invokePerformLogin();

        assertTrue(result);
        assertEquals(1, fakeManager.loginCallCount);
        assertEquals("admin", fakeManager.lastId);

        printPass("숫자로 시작하는 ID 입력 시 재입력 처리 테스트 통과");
    }

    @Test
    @DisplayName("빈 ID 입력 시 예외 발생")
    void performLoginThrowsWhenEmptyIdIsEntered() throws Exception {
        FakeLibraryManager fakeManager = new FakeLibraryManager();

        setInput("\n");
        setStaticField("manager", fakeManager);

        StringIndexOutOfBoundsException exception = assertThrows(
                StringIndexOutOfBoundsException.class,
                this::invokePerformLogin
        );

        assertNotNull(exception);
        assertEquals(0, fakeManager.loginCallCount);

        printPass("빈 ID 입력 시 예외 발생 테스트 통과");
    }

    private void printPass(String message) {
        originalOut.println("[PASS] LibraryMainTest - " + message);
    }

    private void setInput(String input) throws Exception {
        ByteArrayInputStream testInput = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        System.setIn(testInput);
        setStaticField("sc", new Scanner(System.in));
    }

    private boolean invokePerformLogin() throws Exception {
        Method method = LibraryMain.class.getDeclaredMethod("performLogin");
        method.setAccessible(true);

        try {
            return (boolean) method.invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private void setStaticField(String fieldName, Object value) throws Exception {
        Field field = LibraryMain.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static class FakeLibraryManager extends LibraryManager {
        private String acceptedId;
        private String acceptedPassword;
        private int loginCallCount;
        private String lastId;
        private String lastPassword;

        private FakeLibraryManager() {
            super(new LibraryRepository());
        }

        @Override
        public boolean login(String id, String pw) {
            loginCallCount++;
            lastId = id;
            lastPassword = pw;
            return id.equals(acceptedId) && pw.equals(acceptedPassword);
        }
    }
}
