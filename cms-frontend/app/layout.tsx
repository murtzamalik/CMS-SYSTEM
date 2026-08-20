import { Inter } from 'next/font/google';
import ClientRoot from './ClientRoot';
import 'primereact/resources/primereact.css';
import 'primeflex/primeflex.css';
import 'primeicons/primeicons.css';
import '../styles/layout/layout.scss';

const inter = Inter({
    subsets: ['latin'],
    weight: ['400', '500', '600', '700'],
    display: 'swap',
    variable: '--font-inter',
});

interface RootLayoutProps {
    children: React.ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps) {
    return (
        <html lang="en" className={inter.variable} suppressHydrationWarning>
            <head>
                <link id="theme-css" href={`/themes/fintech-light/theme.css`} rel="stylesheet" />
            </head>
            <body className={inter.className}>
                <ClientRoot>{children}</ClientRoot>
            </body>
        </html>
    );
}
