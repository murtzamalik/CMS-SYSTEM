import { Metadata } from 'next';
import React from 'react';

interface SimpleLayoutProps {
    children: React.ReactNode;
}

export const metadata: Metadata = {
    title: 'CMS — Sign in',
    description: 'Card Management System'
};

export default function SimpleLayout({ children }: SimpleLayoutProps) {
    return <React.Fragment>{children}</React.Fragment>;
}
