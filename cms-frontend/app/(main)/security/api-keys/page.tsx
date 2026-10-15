'use client';

import React, { useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { DataTable } from 'primereact/datatable';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Toolbar } from 'primereact/toolbar';
import { apiKeyService, type ApiKeyResponse } from '@/services/security/ApiKeyService';

export default function ApiKeysPage() {
  const toast = useRef<Toast>(null);
  const qc = useQueryClient();
  const [secretDialog, setSecretDialog] = useState<string | null>(null);

  const { data: keys = [], isLoading } = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => apiKeyService.list(),
  });

  const activeKey = useMemo(() => keys.find((k) => k.status === 'ACTIVE') ?? null, [keys]);

  const rotateMutation = useMutation({
    mutationFn: () => apiKeyService.rotate(),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['api-keys'] });
      if (created.secret) {
        setSecretDialog(created.secret);
      }
      toast.current?.show({
        severity: 'success',
        summary: 'API key generated',
        detail: 'Copy the secret now — it will not be shown again.',
        life: 5000,
      });
    },
    onError: (err: Error) => {
      toast.current?.show({
        severity: 'error',
        summary: 'Generate failed',
        detail: err.message,
        life: 5000,
      });
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (id: number) => apiKeyService.revoke(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['api-keys'] });
      toast.current?.show({
        severity: 'success',
        summary: 'Revoked',
        detail: 'API key has been revoked.',
        life: 3000,
      });
    },
    onError: (err: Error) => {
      toast.current?.show({
        severity: 'error',
        summary: 'Revoke failed',
        detail: err.message,
        life: 5000,
      });
    },
  });

  const confirmRotate = () => {
    confirmDialog({
      header: 'Generate / rotate API key',
      message: activeKey
        ? 'This will revoke the current active key. The mobile app must be updated with the new key or all cms-app calls will fail.'
        : 'Generate the first channel API key for cms-app (mobile).',
      icon: 'pi pi-exclamation-triangle',
      acceptClassName: 'p-button-danger',
      acceptLabel: activeKey ? 'Revoke & generate' : 'Generate',
      accept: () => rotateMutation.mutate(),
    });
  };

  const confirmRevoke = (row: ApiKeyResponse) => {
    confirmDialog({
      header: 'Revoke API key',
      message: `Revoke key ${row.keyPrefix}…? Mobile calls using this key will stop working.`,
      icon: 'pi pi-exclamation-triangle',
      acceptClassName: 'p-button-danger',
      acceptLabel: 'Revoke',
      accept: () => revokeMutation.mutate(row.id),
    });
  };

  const copySecret = async () => {
    if (!secretDialog) return;
    try {
      await navigator.clipboard.writeText(secretDialog);
      toast.current?.show({ severity: 'info', summary: 'Copied', life: 2000 });
    } catch {
      toast.current?.show({
        severity: 'warn',
        summary: 'Copy failed',
        detail: 'Select and copy the key manually.',
        life: 4000,
      });
    }
  };

  const statusBody = (row: ApiKeyResponse) => (
    <Tag
      value={row.status}
      severity={row.status === 'ACTIVE' ? 'success' : 'danger'}
    />
  );

  const prefixBody = (row: ApiKeyResponse) => (
    <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
      {row.keyPrefix}••••••••
    </span>
  );

  const dateBody = (value?: string) =>
    value ? new Date(value).toLocaleString() : '—';

  const actionsBody = (row: ApiKeyResponse) =>
    row.status === 'ACTIVE' ? (
      <Button
        label="Revoke"
        icon="pi pi-ban"
        className="p-button-text p-button-danger p-button-sm"
        onClick={() => confirmRevoke(row)}
        disabled={revokeMutation.isPending}
      />
    ) : null;

  return (
    <div className="grid">
      <Toast ref={toast} />
      <ConfirmDialog />
      <div className="col-12">
        <div className="card">
          <h5 className="m-0 mb-1">API Keys</h5>
          <p className="m-0 mb-3 text-color-secondary" style={{ fontSize: '0.875rem' }}>
            Channel key for cms-app (mobile). Only one key is active at a time. End users still log in with
            username/password; this key authenticates the mobile client.
          </p>

          <div
            className="mb-3 p-3 border-round"
            style={{ background: 'var(--surface-ground)', border: '1px solid var(--surface-border)' }}
          >
            <div className="text-color-secondary mb-1" style={{ fontSize: '0.75rem' }}>
              Current active key
            </div>
            {activeKey ? (
              <div className="flex align-items-center gap-3 flex-wrap">
                <span style={{ fontFamily: 'monospace' }}>{activeKey.keyPrefix}••••••••</span>
                <Tag value="ACTIVE" severity="success" />
                <span className="text-color-secondary" style={{ fontSize: '0.85rem' }}>
                  Created {dateBody(activeKey.createdAt)} by {activeKey.createdBy || '—'}
                </span>
              </div>
            ) : (
              <span className="text-color-secondary">No active key — generate one before using the mobile app.</span>
            )}
          </div>

          <Toolbar
            className="mb-3"
            left={
              <Button
                label={activeKey ? 'Rotate key' : 'Generate key'}
                icon="pi pi-refresh"
                onClick={confirmRotate}
                loading={rotateMutation.isPending}
              />
            }
          />

          <DataTable
            value={keys}
            loading={isLoading}
            emptyMessage="No API keys yet."
            paginator
            rows={10}
            dataKey="id"
          >
            <Column header="Key" body={prefixBody} />
            <Column field="status" header="Status" body={statusBody} style={{ width: '8rem' }} />
            <Column field="createdBy" header="Created by" />
            <Column header="Created" body={(r: ApiKeyResponse) => dateBody(r.createdAt)} />
            <Column field="revokedBy" header="Revoked by" body={(r: ApiKeyResponse) => r.revokedBy || '—'} />
            <Column header="Revoked" body={(r: ApiKeyResponse) => dateBody(r.revokedAt)} />
            <Column header="" body={actionsBody} style={{ width: '7rem' }} />
          </DataTable>
        </div>
      </div>

      <Dialog
        header="Copy your new API key"
        visible={!!secretDialog}
        style={{ width: '36rem' }}
        modal
        closable={false}
        onHide={() => setSecretDialog(null)}
        footer={
          <div className="flex justify-content-end gap-2">
            <Button label="Copy" icon="pi pi-copy" onClick={copySecret} />
            <Button
              label="Done — I saved it"
              icon="pi pi-check"
              onClick={() => setSecretDialog(null)}
              autoFocus
            />
          </div>
        }
      >
        <p className="mt-0 mb-3 text-color-secondary" style={{ fontSize: '0.875rem' }}>
          This secret is shown only once. Set it as <code>VITE_CMS_API_KEY</code> (or equivalent) in the mobile
          app. Send it as the <code>X-Api-Key</code> header on every cms-app request.
        </p>
        <InputText value={secretDialog ?? ''} readOnly className="w-full" style={{ fontFamily: 'monospace' }} />
      </Dialog>
    </div>
  );
}
