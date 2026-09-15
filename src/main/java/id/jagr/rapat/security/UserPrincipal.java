package id.jagr.rapat.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.Capability;
import id.jagr.rapat.user.User;

/**
 * Single principal type for both form-login and Google OIDC login, so
 * controllers/services never need to branch on how the user authenticated.
 */
public class UserPrincipal implements UserDetails, OidcUser {

    private final User user;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public UserPrincipal(User user) {
        this(user, Map.of("email", user.getEmail()), null, null);
    }

    public UserPrincipal(User user, OidcUser oidcUser) {
        this(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private UserPrincipal(User user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.user = user;
        this.attributes = attributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public AppRole getRole() {
        return user.getRole();
    }

    public Long getDivisionId() {
        return user.getDivision() == null ? null : user.getDivision().getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : attributes;
    }

    /**
     * Custom roles (issue #46) grant a CAPABILITY_&lt;code&gt; authority per page/menu capability
     * on top of the usual ROLE_&lt;name&gt; -- protected roles never have any capabilities (see
     * AppRole), so this is a pure addition for them. See the 7 controllers' @PreAuthorize for
     * where these are actually checked (issue #47).
     *
     * <p>CAN_ORGANIZE_MEETINGS mirrors AppRole.canOrganizeMeetings directly (not a Capability --
     * it's a domain behavior flag, not a page/menu grant) so MeetingController's HTTP gate can
     * be extended the same way (issue #53) without letting MeetingService.create()'s own
     * division-match check be the only thing standing between a no-division role and a 500.
     */
    @Override
    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        for (Capability capability : user.getRole().getCapabilities()) {
            authorities.add(new SimpleGrantedAuthority("CAPABILITY_" + capability.getCode()));
        }
        if (user.getRole().isCanOrganizeMeetings()) {
            authorities.add(new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS"));
        }
        return authorities;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
