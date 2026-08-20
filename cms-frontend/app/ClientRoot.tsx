'use client';

import { LayoutProvider } from '../layout/context/layoutcontext';
import { PrimeReactProvider } from 'primereact/api';
import { Toast } from 'primereact/toast';
import { useRef } from 'react';
import { Providers } from './providers';

export default function ClientRoot({ children }: { children: React.ReactNode }) {
    const toast = useRef<Toast>(null);
    return (
        <PrimeReactProvider>
            <Providers>
                <Toast ref={toast} />
                <LayoutProvider>{children}</LayoutProvider>
            </Providers>
        </PrimeReactProvider>
    );
}
