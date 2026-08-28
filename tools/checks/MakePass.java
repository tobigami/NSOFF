/** Generates a password hash with the very library the server verifies against. */
public final class MakePass {
    public static void main(String[] a) {
        System.out.println(com.nsoz.util.StringUtils.genPass(a[0]));
    }
}
