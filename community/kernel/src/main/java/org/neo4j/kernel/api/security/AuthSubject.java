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
package org.neo4j.kernel.api.security;

import java.io.IOException;

import org.neo4j.graphdb.security.AuthorizationViolationException;
import org.neo4j.kernel.api.exceptions.InvalidArgumentsException;
import org.neo4j.kernel.impl.api.security.AccessModeSnapshot;

public interface AuthSubject extends AccessMode
{
    void logout();

    // TODO: Refine this API into something more polished
    AuthenticationResult getAuthenticationResult();

    /**
     * Set the password for the AuthSubject
     * @param password The new password
     * @param requirePasswordChange
     * @throws IOException If the new credentials cannot be serialized to disk.
     * @throws InvalidArgumentsException If the new password is invalid.
     */
    void setPassword( String password, boolean requirePasswordChange ) throws IOException, InvalidArgumentsException;

    /**
     * Changes the {@link AuthenticationResult} status to {@link AuthenticationResult#SUCCESS SUCCESS} if it was {@link AuthenticationResult#PASSWORD_CHANGE_REQUIRED PASSWORD_CHANGE_REQUIRED}.
     * This allows users that changed their password to become authorized for continued processing.
     */
    void setPasswordChangeNoLongerRequired();

    /**
     * @param username a username
     * @return true if the provided username is the underlying user name of this subject
     */
    boolean hasUsername( String username );

    /**
     * Ensure that the provided username is the name of an existing user known to the system.
     *
     * @param username a username
     * @throws InvalidArgumentsException if the provided user name is not the name of an existing user
     */
    default void ensureUserExistsWithName( String username ) throws InvalidArgumentsException {
        throw new InvalidArgumentsException( "User '" + username + "' does not exit." );
    }

    @Override
    default AccessMode getSnapshot()
    {
        return AccessModeSnapshot.create( this );
    }

    abstract class StaticAccessModeAdapter implements AuthSubject
    {
        private final AccessMode accessMode;

        public StaticAccessModeAdapter( AccessMode.Static accessMode )
        {
            this.accessMode = accessMode;
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
        public boolean isOverridden()
        {
            return accessMode.isOverridden();
        }

        @Override
        public AuthorizationViolationException onViolation( String msg )
        {
            return accessMode.onViolation( msg );
        }

        @Override
        public String name()
        {
            return accessMode.name();
        }
    }

    /**
     * Implementation to use when authentication has not yet been performed. Allows nothing.
     */
    AuthSubject ANONYMOUS = new StaticAccessModeAdapter( Static.NONE )
    {
        @Override
        public void logout()
        {
        }

        @Override
        public AuthenticationResult getAuthenticationResult()
        {
            return AuthenticationResult.FAILURE;
        }

        @Override
        public void setPassword( String password, boolean requirePasswordChange )
                throws IOException, InvalidArgumentsException
        {
            throw new AuthorizationViolationException( "Anonymous cannot change password" );
        }

        @Override
        public void setPasswordChangeNoLongerRequired()
        {
        }

        @Override
        public boolean hasUsername( String username )
        {
            return false;
        }

        @Override
        public boolean allowsProcedureWith( String[] roleNames )
        {
            return false;
        }

        @Override
        public String name()
        {
            return "<anonymous>";
        }
    };

    /**
     * Implementation to use when authentication is disabled. Allows everything.
     */
    AuthSubject AUTH_DISABLED = new StaticAccessModeAdapter( Static.FULL )
    {
        @Override
        public String name()
        {
            return "<auth disabled>";
        }

        @Override
        public void logout()
        {
        }

        @Override
        public AuthenticationResult getAuthenticationResult()
        {
            return AuthenticationResult.SUCCESS;
        }

        @Override
        public void setPassword( String password, boolean requirePasswordChange )
                throws IOException, InvalidArgumentsException
        {
        }

        @Override
        public void setPasswordChangeNoLongerRequired()
        {
        }

        @Override
        public boolean hasUsername( String username )
        {
            return false;
        }

        @Override
        public boolean allowsProcedureWith( String[] roleNames )
        {
            return true;
        }
    };
}
