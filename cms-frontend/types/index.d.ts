import React, { ReactNode } from 'react';
import {
    AppBreadcrumbProps,
    Breadcrumb,
    BreadcrumbItem,
    MenuProps,
    MenuModel,
    LayoutConfig,
    LayoutState,
    LayoutContextProps,
    MenuContextProps,
    AppConfigProps,
    NodeRef,
    AppTopbarRef,
    AppMenuItemProps,
    AppMenuItem
} from './layout';
import type {
    CustomEvent,
    ChartDataState,
    ChartOptionsState,
    Page,
    LayoutType,
    SortOrderType
} from './demo';

type ChildContainerProps = {
    children: ReactNode;
};

export type {
    AppBreadcrumbProps,
    Breadcrumb,
    BreadcrumbItem,
    MenuProps,
    MenuModel,
    LayoutConfig,
    LayoutState,
    LayoutContextProps,
    MenuContextProps,
    AppConfigProps,
    NodeRef,
    AppTopbarRef,
    AppMenuItemProps,
    ChildContainerProps,
    AppMenuItem,
    CustomEvent,
    ChartDataState,
    ChartOptionsState,
    Page,
    LayoutType,
    SortOrderType
};

export { Demo } from './demo';
