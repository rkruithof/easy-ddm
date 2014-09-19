package nl.knaw.dans.easy.business.authn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import nl.knaw.dans.common.lang.RepositoryException;
import nl.knaw.dans.common.lang.repo.exception.ObjectNotInStoreException;
import nl.knaw.dans.easy.data.Data;
import nl.knaw.dans.easy.domain.authn.Authentication;
import nl.knaw.dans.easy.domain.authn.ForgottenPasswordMailAuthentication;
import nl.knaw.dans.easy.domain.authn.RegistrationMailAuthentication;
import nl.knaw.dans.easy.domain.authn.UsernamePasswordAuthentication;
import nl.knaw.dans.easy.domain.model.user.EasyUser;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationSpecification {
    private static Logger logger = LoggerFactory.getLogger(AuthenticationSpecification.class);

    private File masterkeyPasswordFile;

    /**
     * Check against authentication criteria. <br/>
     * !SIDE EFFECT: the user maybe loaded onto authentication if everything is well.
     * 
     * @param authentication
     *        check this authentication
     * @return the authentication in a certain state
     */
    public boolean isSatisfiedBy(UsernamePasswordAuthentication authentication) {
        return hasSufficientData(authentication) && isAuthenticated(authentication) && userIsInQualifiedState(authentication);
    }

    private boolean hasSufficientData(UsernamePasswordAuthentication authentication) {
        boolean sufficientData = true;
        if (StringUtils.isBlank(authentication.getUserId())) {
            sufficientData = false;
            authentication.setState(Authentication.State.UserIdConnotBeBlank);
            logger.debug("userId cannot be blank " + authentication.toString());
        }
        if (StringUtils.isBlank(authentication.getCredentials())) {
            sufficientData = false;
            authentication.setState(Authentication.State.CredentialsCannotBeBlank);
            logger.debug("credentials cannot be blank " + authentication.toString());
        }
        return sufficientData;
    }

    private boolean isAuthenticated(UsernamePasswordAuthentication authentication) {
        boolean authenticated = false;
        String userId = authentication.getUserId();
        String password = authentication.getCredentials();
        try {
            if (DigestUtils.sha1Hex(password).equals(getMasterkeyPasswordHash())) {
                logger.warn("LOGGED IN WITH MASTERKEY AS USER {} !!!", userId);
                authenticated = true;
            } else {
                authenticated = Data.getUserRepo().authenticate(userId, password);
            }
            if (!authenticated) {
                authentication.setState(Authentication.State.InvalidUsernameOrCredentials);
                logger.debug("Invalid userId or credentials for user " + userId);
            }
        }
        catch (RepositoryException e) {
            logger.error("Could not authenticate user with userId '" + userId, e);
            authentication.setState(Authentication.State.SystemError, e);
        }
        return authenticated;
    }

    // SIDE EFFECT: user may be loaded onto authentication
    public boolean userIsInQualifiedState(Authentication authentication) {
        boolean isInQualifiedState = false;
        EasyUser user = loadUser(authentication.getUserId());
        if (user.isAnonymous()) {
            authentication.setState(Authentication.State.NotFound);
            return isInQualifiedState;
        }

        if (checkUserState(authentication, user)) {
            isInQualifiedState = true;
            authentication.setUser(user);
        } else {
            authentication.setState(Authentication.State.NotQualified);
            logger.warn("Attempt to authenticate while in unqualified state: " + authentication.toString());
        }
        return isInQualifiedState;
    }

    private boolean checkUserStateForUsernamePassword(final EasyUser user) {
        return EasyUser.State.ACTIVE.equals(user.getState()) || EasyUser.State.CONFIRMED_REGISTRATION.equals(user.getState());
    }

    private boolean checkUserStateForRegistration(final EasyUser user) {
        return EasyUser.State.REGISTERED.equals(user.getState());
    }

    public boolean checkUserStateForForgottenPassword(final EasyUser user) {
        return EasyUser.State.ACTIVE.equals(user.getState()) || EasyUser.State.CONFIRMED_REGISTRATION.equals(user.getState());
    }

    private boolean checkUserState(final Authentication authentication, final EasyUser user) {
        if (authentication instanceof UsernamePasswordAuthentication) {
            return checkUserStateForUsernamePassword(user);
        } else if (authentication instanceof RegistrationMailAuthentication) {
            return checkUserStateForRegistration(user);
        } else if (authentication instanceof ForgottenPasswordMailAuthentication) {
            return checkUserStateForForgottenPassword(user);
        } else {
            final String msg = "Unknown type of authentication: " + authentication.getClass().getName()
                    + "\n\tBetter make sure there is a method for this type of authentication!";
            logger.error(msg);
            authentication.setState(Authentication.State.SystemError);
            throw new IllegalStateException(msg);
        }
    }

    private EasyUser loadUser(String userId) {
        EasyUser user = null;
        try {
            user = Data.getUserRepo().findById(userId);
        }
        catch (ObjectNotInStoreException e) {
            logger.error("User with userId'" + userId + "' not found after authentication: ", e);
        }
        catch (RepositoryException e) {
            logger.error("Loading user with userId '" + userId + "' failed: ", e);
        }
        return user;
    }

    private String getMasterkeyPasswordHash() {
        if (masterkeyPasswordFile != null && masterkeyPasswordFile.exists()) {
            logger.warn("Masterkey password file present");
            BufferedReader lineReader = null;
            try {
                lineReader = new BufferedReader(new FileReader(masterkeyPasswordFile));
                String hash = lineReader.readLine();
                if (hash != null && hash.trim().length() > 0) {
                    logger.warn("Masterkey password hash found: {}", hash);
                    return hash;
                }
                logger.warn("Masterkey password file found, but was empty");
            }
            catch (IOException e) {
                logger.error("Could not read master key hash", e);
            }
            finally {
                IOUtils.closeQuietly(lineReader);
            }
        } else {
            logger.debug("No masterkey present");
        }
        return null;
    }

    public void setMasterkeyPasswordFile(File masterkeyPasswordFile) {
        this.masterkeyPasswordFile = masterkeyPasswordFile;
    }
}
