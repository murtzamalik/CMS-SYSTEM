/* eslint-disable @next/next/no-img-element */

import React, { useContext } from 'react';
import { LayoutContext } from './context/layoutcontext';

const AppFooter = () => {
    const { layoutConfig } = useContext(LayoutContext);
    const mark = layoutConfig.colorScheme === 'light' ? 'dark' : 'white';

    return (
        <div className="layout-footer">
            <img
                src={`/layout/images/logo-mark-${mark}.svg`}
                alt="CMS"
                height="22"
                style={{ marginRight: '0.35rem' }}
            />
            <span className="font-semibold">CMS</span>
        </div>
    );
};

export default AppFooter;
