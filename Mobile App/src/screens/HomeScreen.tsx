import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cardInquiry } from '../api/cms';
import { PlasticCard } from '../components/PlasticCard';
import { Banner, PageShell, PrimaryButton, SecondaryButton } from '../components/ui';
import {
  clearSession,
  formatExpiryMonthYear,
  getSession,
  markPinSet,
  maskPan,
  resolvePinSet,
  saveCardContext,
  updateRelationship,
} from '../lib/session';
import type { CardInquiryResponse } from '../types/cms';

export function HomeScreen() {
  const navigate = useNavigate();
  const session = getSession();
  const [rel, setRel] = useState(session?.relationshipNum || '');
  const [cards, setCards] = useState<CardInquiryResponse[]>([]);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async (relationshipNum: string) => {
    setLoading(true);
    setError('');
    setMessage('');
    setPending(false);
    setCards([]);
    try {
      const result = await cardInquiry(relationshipNum);
      if (result.code === 13) {
        setPending(true);
        setMessage(result.message);
        saveCardContext(null);
      } else if (result.code === 3) {
        setMessage(result.message || 'No card found for this relationship.');
        saveCardContext(null);
      } else if (result.cards.length > 0) {
        const loaded = result.cards.map((item) => {
          const pinSet = resolvePinSet(item.pan, item.pinSet);
          if (pinSet) {
            markPinSet(item.pan);
          }
          return { ...item, pinSet };
        });
        setCards(loaded);
        const first = loaded[0];
        saveCardContext({
          pan: first.pan,
          relationshipNum,
          cardTitle: first.cardTitle,
          pinSet: Boolean(first.pinSet),
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Inquiry failed');
      saveCardContext(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (session?.relationshipNum) {
      void load(session.relationshipNum);
    }
  }, [load, session?.relationshipNum]);

  function refresh() {
    const value = rel.trim();
    if (value.length !== 13) {
      setError('Relationship number must be 13 digits.');
      return;
    }
    updateRelationship(value);
    void load(value);
  }

  function logout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <PageShell
      title="My card"
      subtitle={session?.fullName || 'Welcome'}
      action={
        <button type="button" className="text-btn" onClick={logout}>
          Logout
        </button>
      }
    >
      {cards.length === 0 ? (
        <PlasticCard pending={pending} emptyMessage={message} />
      ) : (
        cards.map((item) => (
          <PlasticCard key={item.pan || item.cardTypeCode} card={item} />
        ))
      )}

      {error ? <Banner kind="error">{error}</Banner> : null}
      {!error && message && !pending && cards.length === 0 ? (
        <Banner kind="info">{message}</Banner>
      ) : null}

      <section className="panel">
        <label className="field">
          <span>Relationship number</span>
          <div className="row-input">
            <input
              inputMode="numeric"
              maxLength={13}
              value={rel}
              onChange={(e) =>
                setRel(e.target.value.replace(/\D/g, '').slice(0, 13))
              }
            />
            <SecondaryButton type="button" onClick={refresh} disabled={loading}>
              {loading ? '…' : 'Refresh'}
            </SecondaryButton>
          </div>
        </label>
      </section>

      {cards.map((item) => (
        <section className="panel details" key={`details-${item.pan || item.cardTypeCode}`}>
          <h3>{item.cardTypeName || item.cardTypeCode || 'Card'} details</h3>
          <dl>
            <div>
              <dt>PAN</dt>
              <dd>{maskPan(item.pan)}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <span className="status-chip">{item.cardStatusCode || '—'}</span>
              </dd>
            </div>
            <div>
              <dt>PIN</dt>
              <dd>{resolvePinSet(item.pan, item.pinSet) ? 'Set' : 'Not set'}</dd>
            </div>
            <div>
              <dt>Production</dt>
              <dd>{item.cardProdStatus || '—'}</dd>
            </div>
            <div>
              <dt>Expiry</dt>
              <dd>{formatExpiryMonthYear(item.expiryDate)}</dd>
            </div>
            <div>
              <dt>Type</dt>
              <dd>{item.cardTypeName || item.cardTypeCode || '—'}</dd>
            </div>
          </dl>
          <div className="action-grid">
            <SecondaryButton
              type="button"
              onClick={() => {
                saveCardContext({
                  pan: item.pan,
                  relationshipNum: rel.trim(),
                  cardTitle: item.cardTitle,
                  pinSet: Boolean(item.pinSet),
                });
                navigate('/pin');
              }}
            >
              Manage PIN
            </SecondaryButton>
            <SecondaryButton
              type="button"
              onClick={() => {
                saveCardContext({
                  pan: item.pan,
                  relationshipNum: rel.trim(),
                  cardTitle: item.cardTitle,
                  pinSet: Boolean(item.pinSet),
                });
                navigate('/status');
              }}
            >
              Change status
            </SecondaryButton>
          </div>
        </section>
      ))}

      <div className="action-grid">
        <PrimaryButton type="button" onClick={() => navigate('/request')}>
          Request card
        </PrimaryButton>
        <SecondaryButton type="button" onClick={() => navigate('/limits')}>
          View limits
        </SecondaryButton>
      </div>
    </PageShell>
  );
}
