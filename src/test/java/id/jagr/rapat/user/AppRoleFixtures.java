package id.jagr.rapat.user;

/** Built-in {@link AppRole} instances matching the seed values in {@code 014-create-app-role-table.xml} exactly. */
public final class AppRoleFixtures {

    public static AppRole admin() {
        return new AppRole(BuiltInRoleNames.ADMIN, true, false, false, true, true);
    }

    public static AppRole direktur() {
        return new AppRole(BuiltInRoleNames.DIREKTUR, true, false, true, true, false);
    }

    public static AppRole ketuaDivisi() {
        return new AppRole(BuiltInRoleNames.KETUA_DIVISI, true, true, false, false, true);
    }

    public static AppRole karyawan() {
        return new AppRole(BuiltInRoleNames.KARYAWAN, true, true, false, false, false);
    }

    private AppRoleFixtures() {
    }
}
