package id.jagr.rapat.user;

/**
 * Names of the 4 {@link AppRole} rows seeded by Liquibase -- the only place these are still
 * hardcoded, for the handful of behaviors that don't map to one of {@link AppRole}'s flags
 * (e.g. {@code MeetingService.canRetrySync}'s Admin-override, which is deliberately narrower
 * than {@code canViewAllDivisions} since Direktur doesn't get it).
 */
public final class BuiltInRoleNames {

    public static final String ADMIN = "ADMIN";
    public static final String DIREKTUR = "DIREKTUR";
    public static final String KETUA_DIVISI = "KETUA_DIVISI";
    public static final String KARYAWAN = "KARYAWAN";

    private BuiltInRoleNames() {
    }
}
