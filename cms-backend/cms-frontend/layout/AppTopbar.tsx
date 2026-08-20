/* eslint-disable @next/next/no-img-element */

'use client';

import Link from 'next/link';
import { classNames } from 'primereact/utils';
import React, { forwardRef, useContext, useImperativeHandle, useMemo, useRef } from 'react';
import { AppTopbarRef } from '@/types';
import { LayoutContext } from './context/layoutcontext';
import { useAuth } from '@/services/auth/AuthContext';

const AppTopbar = forwardRef<AppTopbarRef>((props, ref) => {
    const { layoutConfig, layoutState, onMenuToggle, showProfileSidebar } = useContext(LayoutContext);
    const { user, logout } = useAuth();
    const menubuttonRef = useRef(null);
    const topbarmenuRef = useRef(null);
    const topbarmenubuttonRef = useRef(null);

    useImperativeHandle(ref, () => ({
        menubutton: menubuttonRef.current,
        topbarmenu: topbarmenuRef.current,
        topbarmenubutton: topbarmenubuttonRef.current
    }));

    const displayName = useMemo(() => {
        if (!user) return null;
        const name = user.fullName?.trim();
        return name || user.loginId || null;
    }, [user]);

    const roleLabel = useMemo(() => {
        if (!user?.roles?.length) return null;
        return user.roles.join(', ');
    }, [user]);

    const handleLogout = () => {
        logout();
    };

    return (
        <div className="layout-topbar">
            <Link href="/" className="layout-topbar-logo">
                <img
                  src={`/layout/images/logo-mark-${layoutConfig.colorScheme !== 'light' ? 'white' : 'dark'}.svg`}
                  height={48}
                  width={48}
                  alt="CMS"
                />
                <span>CMS</span>
            </Link>

            <button ref={menubuttonRef} type="button" className="p-link layout-menu-button layout-topbar-button" onClick={onMenuToggle}>
                <i className="pi pi-bars" />
            </button>

            <button ref={topbarmenubuttonRef} type="button" className="p-link layout-topbar-menu-button layout-topbar-button" onClick={showProfileSidebar}>
                <i className="pi pi-ellipsis-v" />
            </button>

            <div ref={topbarmenuRef} className={classNames('layout-topbar-menu', { 'layout-topbar-menu-mobile-active': layoutState.profileSidebarVisible })}>
                {displayName && (
                    <div className="layout-topbar-user" title={roleLabel ? `${displayName} · ${roleLabel}` : displayName}>
                        <div className="layout-topbar-user-avatar" aria-hidden>
                            <i className="pi pi-user" />
                        </div>
                        <div className="layout-topbar-user-text">
                            <span className="layout-topbar-user-name">{displayName}</span>
                            {roleLabel ? (
                                <span className="layout-topbar-user-role">{roleLabel}</span>
                            ) : user?.loginId && user.fullName?.trim() ? (
                                <span className="layout-topbar-user-role">{user.loginId}</span>
                            ) : null}
                        </div>
                    </div>
                )}

                <button
                    type="button"
                    className="p-link layout-topbar-button layout-topbar-logout"
                    onClick={handleLogout}
                    aria-label="Log out"
                >
                    <i className="pi pi-sign-out" />
                    <span>Logout</span>
                </button>
            </div>
        </div>
    );
});

AppTopbar.displayName = 'AppTopbar';

export default AppTopbar;
