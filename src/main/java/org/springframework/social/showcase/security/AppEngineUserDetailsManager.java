package org.springframework.social.showcase.security;

import com.google.appengine.api.datastore.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.inject.Inject;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 2013/10/28
 * Time: 下午 11:44
 * To change this template use File | Settings | File Templates.
 */
public class AppEngineUserDetailsManager implements UserDetailsManager {

    private final DatastoreService db;

    @Inject
    public AppEngineUserDetailsManager() {
        this.db = DatastoreServiceFactory.getDatastoreService();
    }

    @Override
    public void createUser(UserDetails userDetails) {
        Entity e = new Entity( "account", userDetails.getUsername() );
        e.setProperty( "password", userDetails.getPassword() );
        e.setProperty( "enabled", userDetails.isEnabled() );
        db.put( e );
    }

    @Override
    public void updateUser(UserDetails userDetails) {
        Entity e = new Entity( "account", userDetails.getUsername() );
        e.setProperty( "password", userDetails.getPassword() );
        e.setProperty( "enabled", userDetails.isEnabled() );
        db.put( e );
    }

    @Override
    public void deleteUser(String s) {
        Key k = KeyFactory.createKey("account", s);
        db.delete( k );
    }

    @Override
    public void changePassword(String s, String s2) {
        Key k = KeyFactory.createKey( "account", s );
        try {
            Entity e = db.get( k );
            e.setProperty( "password", s2 );
            db.put( e );
        }
        catch ( EntityNotFoundException ex ) {
        }
    }

    @Override
    public boolean userExists(String s) {
        Key k = KeyFactory.createKey( "account", s );
        try {
            Entity e = db.get( k );
            return e != null;
        }
        catch ( EntityNotFoundException ex ) {
        }
        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        Key k = KeyFactory.createKey( "account", s );
        try {
            Entity e = db.get( k );
            String password = (String)e.getProperty( "password" );
            Boolean enabled = (Boolean)e.getProperty( "enabled" );
            User user = new User( s, password, enabled == null ? true : enabled, true, true, true, Collections.EMPTY_LIST );
            return user;
        }
        catch ( EntityNotFoundException ex ) {
        }
        return null;
    }
}
