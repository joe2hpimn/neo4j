/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.security.auth;

import java.io.IOException;

import org.neo4j.graphdb.security.AuthorizationViolationException;
import org.neo4j.kernel.api.security.AccessMode;
import org.neo4j.kernel.api.security.AuthSubject;
import org.neo4j.kernel.api.security.AuthenticationResult;
import org.neo4j.kernel.api.exceptions.InvalidArgumentsException;

import static org.neo4j.kernel.api.security.AuthenticationResult.FAILURE;
import static org.neo4j.kernel.api.security.AuthenticationResult.PASSWORD_CHANGE_REQUIRED;
import static org.neo4j.kernel.api.security.AuthenticationResult.SUCCESS;

public class BasicAuthSubject implements AuthSubject
{
    private final BasicAuthManager authManager;
    private User user;
    private AuthenticationResult authenticationResult;
    private AccessMode.Static accessMode;

    public static BasicAuthSubject castOrFail( AuthSubject authSubject )
    {
        if ( !(authSubject instanceof BasicAuthSubject) )
        {
            throw new IllegalArgumentException( "Incorrect AuthSubject type " + authSubject.getClass().getTypeName() );
        }
        return (BasicAuthSubject) authSubject;
    }

    public BasicAuthSubject( BasicAuthManager authManager, User user, AuthenticationResult authenticationResult )
    {
        this.authManager = authManager;
        this.user = user;
        this.authenticationResult = authenticationResult;

        switch ( authenticationResult )
        {
        case SUCCESS:
            accessMode = Static.FULL;
            break;
        case PASSWORD_CHANGE_REQUIRED:
            accessMode = Static.CREDENTIALS_EXPIRED;
            break;
        default:
            accessMode = Static.NONE;
        }
    }

    @Override
    public void logout()
    {
        user = null;
        authenticationResult = FAILURE;
    }

    @Override
    public AuthenticationResult getAuthenticationResult()
    {
        return authenticationResult;
    }

    /**
     * Sets a new password for the BasicAuthSubject.
     *
     * @param password The new password
     * @param requirePasswordChange
     * @throws IOException If the new user credentials cannot be stored on disk.
     * @throws InvalidArgumentsException If password is invalid, e.g. if the new password is the same as the current.
     */
    @Override
    public void setPassword( String password, boolean requirePasswordChange )
            throws IOException, InvalidArgumentsException
    {
        authManager.setPassword( this, user.name(), password, requirePasswordChange );

        // Make user authenticated if successful
        setPasswordChangeNoLongerRequired();
    }

    @Override
    public void setPasswordChangeNoLongerRequired()
    {
        if ( authenticationResult == PASSWORD_CHANGE_REQUIRED )
        {
            authenticationResult = SUCCESS;
            accessMode = AccessMode.Static.FULL;
        }
    }

    @Override
    public boolean allowsProcedureWith( String[] roleName )
    {
        return true;
    }

    public BasicAuthManager getAuthManager()
    {
        return authManager;
    }

    @Override
    public boolean hasUsername( String username )
    {
        return username().equals( username );
    }

    @Override
    public boolean allowsReads()
    {
        return accessMode.allowsReads();
    }

    @Override
    public boolean allowsWrites()
    {
        return accessMode.allowsWrites();
    }

    @Override
    public boolean allowsSchemaWrites()
    {
        return accessMode.allowsSchemaWrites();
    }

    @Override
    public AuthorizationViolationException onViolation( String msg )
    {
        return accessMode.onViolation( msg );
    }

    @Override
    public String name()
    {
        return username();
    }

    @Override
    public String username()
    {
        return user.name();
    }
}
