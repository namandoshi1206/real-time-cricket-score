import { useEffect, useState } from 'react';
import { api, send } from './api.js';

const tabs = ['overview', 'live', 'matches', 'scoring', 'scorecards', 'teams', 'players'];
const roles = ['ADMIN', 'SCORER'];
const EMPTY_FEED = { success: false, available: false, stale: false, message: 'Loading live matches...', lastUpdated: null, data: [] };

export default function App() {
  const [page, setPage] = useState('overview');
  const [data, setData] = useState({ matches: [], teams: [], players: [] });
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [scorecards, setScorecards] = useState([]);
  const [liveFeeds, setLiveFeeds] = useState({ live: EMPTY_FEED, upcoming: EMPTY_FEED, recent: EMPTY_FEED });
  const [liveLoading, setLiveLoading] = useState(true);
  const [selectedLiveId, setSelectedLiveId] = useState(null);
  const [credentials, setCredentials] = useState(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function refresh() {
    try {
      const [matches, teams, players] = await Promise.all([api('/matches'), api('/teams'), api('/players')]);
      setData({ matches, teams, players });
      setError('');
    } catch (cause) { setError(cause.message); }
    finally { setLoading(false); }
  }

  async function refreshLiveFeeds() {
    try {
      const [liveFeed, upcomingFeed, recentFeed] = await Promise.all([
        api('/live/matches'), api('/live/upcoming'), api('/live/recent'),
      ]);
      setLiveFeeds({ live: liveFeed, upcoming: upcomingFeed, recent: recentFeed });
    } catch {
      setLiveFeeds((current) => Object.fromEntries(Object.entries(current).map(([key, feed]) => [key, {
        ...feed,
        success: false,
        stale: feed.data.length > 0,
        message: feed.data.length > 0
          ? 'Showing recently updated data; live provider is temporarily unavailable.'
          : 'Live cricket data is temporarily unavailable.',
      }])));
    } finally { setLiveLoading(false); }
  }

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, 15000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    refreshLiveFeeds();
    const timer = window.setInterval(refreshLiveFeeds, 15000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const navigate = (event) => {
      setPage(event.detail);
      setSelectedMatch(null);
    };
    window.addEventListener('cricket:navigate', navigate);
    return () => window.removeEventListener('cricket:navigate', navigate);
  }, []);

  useEffect(() => {
    if (!selectedMatch) return undefined;
    let active = true;
    const load = async () => {
      try {
        const innings = await api(`/matches/${selectedMatch.id}/innings`);
        const scores = await Promise.all(innings.map((entry) => api(`/innings/${entry.id}/score`)));
        if (active) setScorecards(scores);
      } catch (cause) { if (active) setError(cause.message); }
    };
    load();
    const timer = window.setInterval(load, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [selectedMatch]);

  const live = data.matches.filter((match) => match.status === 'LIVE');
  const upcoming = data.matches.filter((match) => match.status === 'UPCOMING');
  const recent = data.matches.filter((match) => match.status === 'COMPLETED').slice(0, 4);
  const canScore = credentials?.roles?.some((role) => roles.includes(role));

  function openMatch(match) {
    setSelectedMatch(match);
    setPage('match');
  }

  function openLiveMatch(match) {
    setSelectedLiveId(match.matchId);
    setPage('live-detail');
  }

  return <div className="shell">
    <aside className="sidebar">
      <button className="wordmark" onClick={() => setPage('overview')}><span>B</span>BOUNDARY<i>.</i></button>
      <div className="rail-caption">MATCH OPERATIONS</div>
      <nav>{tabs.map((tab, index) => <button key={tab} aria-label={tab[0].toUpperCase() + tab.slice(1)} className={page === tab ? 'nav-link selected' : 'nav-link'} onClick={() => { setPage(tab); setSelectedMatch(null); setSelectedLiveId(null); }}><small>0{index + 1}</small><span>{tab === 'live' ? 'Live Matches' : tab === 'scoring' ? 'Scoring' : tab === 'scorecards' ? 'Scorecards' : tab[0].toUpperCase() + tab.slice(1)}</span>{tab === 'live' && liveFeeds.live.data.length > 0 && <i className="nav-pulse" />}</button>)}</nav>
      <div className="sidebar-foot"><span className="api-light" /> SCORE API <b>9090</b><small>LOCAL DESK · 2026</small></div>
    </aside>

    <main className="workspace">
      <header className="toolbar"><div><span>CRICKET OPERATIONS</span><i>/</i><b>{selectedMatch ? 'MATCH CENTER' : page.toUpperCase()}</b></div><div className="toolbar-actions"><button className="text-button" onClick={refresh}>Refresh</button>{credentials ? <button className="user-chip" onClick={() => setCredentials(null)}>{credentials.username}<small>Sign out</small></button> : <button className="login-action" onClick={() => setAuthOpen(true)}>Scorer login ↗</button>}</div></header>
      {error && <div className="alert" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {loading && <div className="loading-note"><i /> Loading score desk</div>}

      {page === 'overview' && <Dashboard live={live} upcoming={upcoming} recent={recent} teams={data.teams} players={data.players} liveFeeds={liveFeeds} liveLoading={liveLoading} onOpen={openMatch} onOpenLive={openLiveMatch} />}
      {page === 'live' && <LiveMatchesPage feeds={liveFeeds} loading={liveLoading} onOpen={openLiveMatch} />}
      {page === 'teams' && <Teams teams={data.teams} credentials={credentials} refresh={refresh} fail={setError} />}
      {page === 'players' && <Players players={data.players} teams={data.teams} credentials={credentials} refresh={refresh} fail={setError} />}
      {page === 'matches' && <Matches matches={data.matches} teams={data.teams} credentials={credentials} refresh={refresh} fail={setError} onOpen={openMatch} />}
      {page === 'scoring' && <ManualMatchList title="Scoring desk" matches={data.matches.filter((match) => ['LIVE', 'UPCOMING'].includes(match.status))} onOpen={openMatch} />}
      {page === 'scorecards' && <ManualMatchList title="Scorecards" matches={data.matches.filter((match) => match.status === 'COMPLETED')} onOpen={openMatch} />}
      {page === 'match' && selectedMatch && <MatchCenter match={selectedMatch} scores={scorecards} players={data.players} teams={data.teams} canScore={canScore} credentials={credentials} refreshScores={async () => { const innings = await api(`/matches/${selectedMatch.id}/innings`); setScorecards(await Promise.all(innings.map((item) => api(`/innings/${item.id}/score`)))); }} fail={setError} />}
      {page === 'live-detail' && selectedLiveId && <LiveMatchDetail matchId={selectedLiveId} onBack={() => setPage('live')} />}
      {!loading && !error && page === 'overview' && data.matches.length === 0 && <Empty title="No fixtures yet" detail="Create teams and schedule the first match to start the season." />}
    </main>

    {authOpen && <AuthDialog close={() => setAuthOpen(false)} fail={setError} authenticated={(identity, login) => { setCredentials({ ...login, roles: identity.roles }); setAuthOpen(false); }} />}
  </div>;
}

function Dashboard({ live, upcoming, recent, teams, players, liveFeeds, liveLoading, onOpen, onOpenLive }) {
  return <div className="content">
    <section className="hero-band"><div className="hero-copy"><span className="overline">MATCHDAY OPERATIONS · SEASON 2026</span><h1>Every ball<br />has a story.</h1><p>Fixtures, live scoring and club records, all on one desk.</p></div><div className="live-feature"><div className="live-heading"><span>LOCAL SCOREBOARD</span><b><i /> {live.length} LOCAL</b></div>{live[0] ? <MatchCard match={live[0]} onOpen={onOpen} /> : <div className="no-live"><b>—</b><span>No manual match in progress</span><small>Score your own fixtures in Matches</small></div>}<button className="live-link" onClick={() => live[0] && onOpen(live[0])}>OPEN SCORING DESK ↗</button></div><div className="hero-serial">CS<br /><b>26</b></div></section>
    <LiveDashboardSection feeds={liveFeeds} loading={liveLoading} onOpen={onOpenLive} />
    <section className="metric-row"><Metric label="LIVE MATCHES" value={live.length} accent /><Metric label="UPCOMING" value={upcoming.length} /><Metric label="TEAMS" value={teams.length} /><Metric label="PLAYERS" value={players.length} /></section>
    <section className="content-section"><div className="section-head"><div><span className="overline">LOCAL FIXTURES</span><h2>Coming up</h2></div><span>{upcoming.length} SCHEDULED</span></div>{upcoming.length ? upcoming.slice(0, 5).map((match) => <FixtureRow key={match.id} match={match} onOpen={onOpen} />) : <Empty title="Nothing on the calendar" detail="Scheduled fixtures will appear here." />}</section>
    <section className="content-section recent-section"><div className="section-head"><div><span className="overline">LOCAL RESULTS</span><h2>From the book</h2></div><span>FINAL SCORES</span></div>{recent.length ? <div className="result-row">{recent.map((match) => <button key={match.id} onClick={() => onOpen(match)}><small>{match.matchType} · {formatDate(match.scheduledDate)}</small><b>{match.teamAName}</b><i>v</i><b>{match.teamBName}</b><span>FINAL ↗</span></button>)}</div> : <Empty title="No completed manual matches" detail="Local final scores appear after a match is completed." />}</section>
  </div>;
}

function Metric({ label, value, accent }) {
  return <div className={accent ? 'metric accent' : 'metric'}><span>{label}</span><b>{String(value).padStart(2, '0')}</b></div>;
}

function LiveDashboardSection({ feeds, loading, onOpen }) {
  return <section className="external-live-section">
    <div className="section-head"><div><span className="overline">REAL-WORLD CRICKET</span><h2>Live from the grounds</h2></div><button className="live-browse" onClick={() => window.dispatchEvent(new CustomEvent('cricket:navigate', { detail: 'live' }))}>ALL LIVE MATCHES ↗</button></div>
    {feeds.live.data.length ? <div className="live-card-grid">{feeds.live.data.slice(0, 3).map((match) => <LiveMatchCard key={match.matchId} match={match} lastUpdated={feeds.live.lastUpdated} onOpen={onOpen} />)}</div>
      : <LiveFeedMessage feed={feeds.live} loading={loading} category="live" />}
    <div className="external-schedule-grid">
      <ExternalFixtureList title="Upcoming matches" feed={feeds.upcoming} loading={loading} category="upcoming" onOpen={onOpen} />
      <ExternalFixtureList title="Recent results" feed={feeds.recent} loading={loading} category="recent" onOpen={onOpen} />
    </div>
  </section>;
}

function ExternalFixtureList({ title, feed, loading, category, onOpen }) {
  return <section className="external-fixture-section"><div className="external-list-heading"><h3>{title}</h3><span>{feed.data.length}</span></div>{feed.data.slice(0, 4).map((match) => <button key={match.matchId} onClick={() => onOpen(match)}><span>{match.title}</span><small>{match.status || 'Data unavailable'} · {match.venue || 'Venue unavailable'}</small></button>)}{!feed.data.length && <LiveFeedMessage feed={feed} loading={loading} category={category} compact />}</section>;
}

function LiveFeedMessage({ feed, loading, category, compact = false }) {
  let message = feed.message;
  if (loading && !feed.lastUpdated) message = 'Loading live matches...';
  else if (feed.stale) message = 'Showing recently updated data.';
  else if (feed.available && !feed.data.length) {
    message = category === 'live' ? 'No live matches currently available.'
      : category === 'upcoming' ? 'No upcoming matches currently available.'
        : 'No recent results currently available.';
  } else if (!feed.available) message = 'Live cricket data is temporarily unavailable.';
  return <div className={compact ? 'live-feed-message compact' : 'live-feed-message'} role="status"><i className={feed.available ? 'feed-dot available' : 'feed-dot'} /><span>{message || 'Live cricket data is temporarily unavailable.'}</span>{feed.lastUpdated && <small>Updated {timeAgo(feed.lastUpdated)}</small>}</div>;
}

function LiveMatchCard({ match, lastUpdated, onOpen }) {
  const score = match.score;
  const isLive = (match.status || '').toUpperCase() === 'LIVE';
  const team1 = match.team1?.shortName || match.team1?.name || 'Data unavailable';
  const team2 = match.team2?.shortName || match.team2?.name || 'Data unavailable';
  return <button className="external-match-card" onClick={() => onOpen(match)}>
    <div className="external-card-top"><span className={isLive ? 'live-status-pill is-live' : 'live-status-pill'}>{isLive && <i />}{match.status || 'Status unavailable'}</span><small>{match.venue || 'Venue unavailable'}</small></div>
    <h3>{match.title || `${team1} vs ${team2}`}</h3>
    <div className="external-score-line"><b>{match.team1?.logoUrl && <img src={match.team1.logoUrl} alt="" loading="lazy" />}{team1}</b><strong>{score?.runs != null && score?.wickets != null ? `${score.runs}/${score.wickets}` : 'Data unavailable'}</strong></div>
    <div className="external-score-line"><b>{match.team2?.logoUrl && <img src={match.team2.logoUrl} alt="" loading="lazy" />}{team2}</b><span>{score?.overs || 'Overs unavailable'}</span></div>
    <div className="external-card-meta">{score?.inningsNumber != null && <span>INNINGS {score.inningsNumber}</span>}{score?.runRate != null && <span>RR {score.runRate}</span>}{score?.targetRuns != null && <span>TARGET {score.targetRuns}</span>}</div>
    <div className="external-card-bottom"><small>Updated {timeAgo(match.updatedAt || lastUpdated)}</small><span>VIEW SCORECARD ↗</span></div>
  </button>;
}

function LiveMatchesPage({ feeds, loading, onOpen }) {
  const [filter, setFilter] = useState('all');
  const [search, setSearch] = useState('');
  const allMatches = [...feeds.live.data, ...feeds.upcoming.data, ...feeds.recent.data]
    .filter((match, index, matches) => matches.findIndex((candidate) => candidate.matchId === match.matchId) === index);
  const matches = (filter === 'all' ? allMatches : feeds[filter].data).filter((match) => {
    const query = search.trim().toLowerCase();
    return !query || [match.title, match.team1?.name, match.team1?.shortName, match.team2?.name, match.team2?.shortName]
      .some((value) => value?.toLowerCase().includes(query));
  });
  const activeFeed = filter === 'all' ? feeds.live : feeds[filter];
  return <div className="content live-page"><PageHeading tag="EXTERNAL MATCH CENTRE" title="Live matches" detail="Scores are provided by the configured cricket data source." />
    <div className="live-tools"><div className="live-filters" role="group" aria-label="Filter live matches">{[['all', 'All'], ['live', 'Live'], ['upcoming', 'Upcoming'], ['recent', 'Recent']].map(([key, label]) => <button key={key} className={filter === key ? 'filter-chip active' : 'filter-chip'} onClick={() => setFilter(key)}>{label}</button>)}</div><label className="live-search"><span>⌕</span><input type="search" aria-label="Search by team" placeholder="Search team" value={search} onChange={(event) => setSearch(event.target.value)} /></label></div>
    {matches.length ? <div className="live-card-grid">{matches.map((match) => <LiveMatchCard key={match.matchId} match={match} lastUpdated={activeFeed.lastUpdated} onOpen={onOpen} />)}</div>
      : search.trim()
        ? <Empty title="No matching teams" detail={`No provider match includes “${search.trim()}”.`} />
        : <LiveFeedMessage feed={activeFeed} loading={loading} category={filter === 'all' ? 'live' : filter} />}
    {activeFeed.lastUpdated && <p className="feed-last-updated">Last updated {timeAgo(activeFeed.lastUpdated)}{activeFeed.stale ? ' · Showing cached data' : ''}</p>}
  </div>;
}

function LiveMatchDetail({ matchId, onBack }) {
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [unavailable, setUnavailable] = useState(false);
  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const next = await api(`/live/matches/${encodeURIComponent(matchId)}`);
        if (active) { setResponse(next); setUnavailable(false); }
      } catch {
        if (active) setUnavailable(true);
      } finally { if (active) setLoading(false); }
    };
    load();
    const timer = window.setInterval(load, 15000);
    return () => { active = false; window.clearInterval(timer); };
  }, [matchId]);
  const match = response?.data;
  return <div className="content live-detail-page"><button className="back-link" onClick={onBack}>← Live matches</button>
    {unavailable && <LiveFeedMessage feed={{ available: false, data: [], message: 'Live cricket data is temporarily unavailable.' }} loading={loading} category="live" />}
    {match && <>
      <section className="external-match-banner"><div><span className={(match.status || '').toUpperCase() === 'LIVE' ? 'live-status-pill is-live' : 'live-status-pill'}>{(match.status || '').toUpperCase() === 'LIVE' && <i />}{match.status || 'Status unavailable'}</span><span>{match.venue || 'Venue unavailable'}</span></div><h1>{match.title}</h1><p>{match.team1?.name || 'Data unavailable'} <i>vs</i> {match.team2?.name || 'Data unavailable'}</p>{match.score && <strong>{match.score.runs ?? '—'}/{match.score.wickets ?? '—'} <small>{match.score.overs || 'Overs unavailable'}</small></strong>}<footer>{match.score?.inningsNumber != null && <span>INNINGS {match.score.inningsNumber}</span>}{match.score?.runRate != null && <span>RUN RATE {match.score.runRate}</span>}{match.score?.targetRuns != null && <span>TARGET {match.score.targetRuns}</span>}<span>Updated {timeAgo(response.lastUpdated || match.updatedAt)}</span></footer></section>
      <LiveScorecard match={match} />
      <section className="detail-data-sections">{match.batsmen?.length > 0 && <div><h2>Batting</h2>{match.batsmen.map((batter) => <p key={batter.id || batter.name}>{batter.name}: {batter.runs ?? '—'} ({batter.balls ?? '—'}){batter.onStrike ? ' *' : ''}</p>)}</div>}{match.bowlers?.length > 0 && <div><h2>Bowling</h2>{match.bowlers.map((bowler) => <p key={bowler.id || bowler.name}>{bowler.name}: {bowler.overs || '—'} ov · {bowler.runs ?? '—'} runs · {bowler.wickets ?? '—'} wickets</p>)}</div>}{match.recentDeliveries?.length > 0 && <div><h2>Recent deliveries</h2><p>{match.recentDeliveries.join('  ·  ')}</p></div>}{match.innings?.length > 0 && <div><h2>Innings</h2>{match.innings.map((innings) => <p key={`${innings.teamId}-${innings.inningsNumber}`}>{innings.teamName || 'Team'}: {innings.runs ?? '—'}/{innings.wickets ?? '—'} · {innings.overs || 'Overs unavailable'}</p>)}</div>}{!match.batsmen?.length && !match.bowlers?.length && !match.recentDeliveries?.length && !match.innings?.length && <div><h2>Additional match data</h2><p>Data unavailable from the configured provider.</p></div>}</section>
      {match.result && <p className="external-result">{match.result}</p>}
      {response.stale && <p className="feed-last-updated">Showing recently updated data. Last updated {timeAgo(response.lastUpdated)}.</p>}
    </>}
    {loading && !match && <LiveFeedMessage feed={EMPTY_FEED} loading category="live" />}
  </div>;
}

function LiveScorecard({ match }) {
  const batters = match.batsmen || [];
  const bowlers = match.bowlers || [];
  return <section className="provider-scorecard"><div className="section-head"><div><span className="overline">PROVIDER SCORECARD</span><h2>Scorecard</h2></div></div>
    {batters.length > 0 && <div className="provider-table"><div><b>BATTER</b><b>R</b><b>B</b><b>4s</b><b>6s</b><b>SR</b></div>{batters.map((batter) => <div key={batter.id || batter.name}><span>{batter.name || 'Data unavailable'}{batter.onStrike ? ' *' : ''}</span><span>{batter.runs ?? '—'}</span><span>{batter.balls ?? '—'}</span><span>{batter.fours ?? '—'}</span><span>{batter.sixes ?? '—'}</span><span>{batter.strikeRate ?? '—'}</span></div>)}</div>}
    {bowlers.length > 0 && <div className="provider-table bowling"><div><b>BOWLER</b><b>O</b><b>M</b><b>R</b><b>W</b><b>ECON</b></div>{bowlers.map((bowler) => <div key={bowler.id || bowler.name}><span>{bowler.name || 'Data unavailable'}</span><span>{bowler.overs ?? '—'}</span><span>{bowler.maidens ?? '—'}</span><span>{bowler.runs ?? '—'}</span><span>{bowler.wickets ?? '—'}</span><span>{bowler.economy ?? '—'}</span></div>)}</div>}
    {!batters.length && !bowlers.length && <p className="provider-no-scorecard">Scorecard data unavailable from the configured provider.</p>}
  </section>;
}

function ManualMatchList({ title, matches, onOpen }) {
  return <div className="content directory-page"><PageHeading tag="MANUAL MATCH MANAGEMENT" title={title} detail="Locally managed fixtures and scorecards remain separate from external live data." />{matches.map((match) => <FixtureRow key={match.id} match={match} onOpen={onOpen} />)}{!matches.length && <Empty title="No matching manual fixtures" detail="Create or update a local match in Matches." />}</div>;
}

function timeAgo(value) {
  if (!value) return 'update time unavailable';
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 5) return 'just now';
  if (seconds < 60) return `${seconds} seconds ago`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`;
}

function MatchCard({ match, onOpen }) {
  return <button className="live-match-card" onClick={() => onOpen(match)}><div>{match.matchType} <i>·</i> {match.venue}</div><strong>{match.teamAName}</strong><span>VS</span><strong>{match.teamBName}</strong><small>{match.title}</small></button>;
}

function FixtureRow({ match, onOpen }) {
  return <button className="fixture" onClick={() => onOpen(match)}><span>{formatDate(match.scheduledDate)}</span><b>{match.teamAName}<i>vs</i>{match.teamBName}</b><small>{match.matchType}</small><span>{match.venue}</span><i>↗</i></button>;
}

function Empty({ title, detail }) {
  return <div className="empty"><b>○</b><div><strong>{title}</strong><span>{detail}</span></div></div>;
}

function PageHeading({ tag, title, detail }) {
  return <div className="page-heading"><span className="overline">{tag}</span><h1>{title}</h1><p>{detail}</p></div>;
}

function Teams({ teams, credentials, refresh, fail }) {
  const [form, setForm] = useState({ name: '', shortName: '', country: '' });
  const writable = credentials?.roles?.some((role) => roles.includes(role));
  async function submit(event) { event.preventDefault(); try { await send('/teams', 'POST', form, credentials); setForm({ name: '', shortName: '', country: '' }); await refresh(); } catch (cause) { fail(cause.message); } }
  async function remove(team) { try { await send(`/teams/${team.id}`, 'DELETE', null, credentials); await refresh(); } catch (cause) { fail(cause.message); } }
  return <div className="content directory-page"><PageHeading tag="CLUB DIRECTORY" title="Teams" detail="The sides behind every scoreline." />{writable && <form className="entry-form" onSubmit={submit}><input required placeholder="Team name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><input required maxLength="20" placeholder="Short code" value={form.shortName} onChange={(event) => setForm({ ...form, shortName: event.target.value.toUpperCase() })} /><input placeholder="Country" value={form.country} onChange={(event) => setForm({ ...form, country: event.target.value })} /><button>Add team +</button></form>}<div className="directory">{teams.map((team, index) => <div key={team.id}><small>{String(index + 1).padStart(2, '0')}</small><b className="monogram">{team.shortName?.slice(0, 3)}</b><span><strong>{team.name}</strong><i>{team.country || 'Independent club'}</i></span><em>{team.shortName}</em>{writable && <button onClick={() => remove(team)} aria-label={`Delete ${team.name}`}>×</button>}</div>)}</div>{!teams.length && <Empty title="No teams registered" detail="Add a team to organize fixtures." />}</div>;
}

function Players({ players, teams, credentials, refresh, fail }) {
  const [form, setForm] = useState({ firstName: '', lastName: '', role: 'BATSMAN', teamId: '' });
  const writable = credentials?.roles?.some((role) => roles.includes(role));
  async function submit(event) { event.preventDefault(); try { await send('/players', 'POST', { ...form, teamId: Number(form.teamId) }, credentials); setForm({ firstName: '', lastName: '', role: 'BATSMAN', teamId: '' }); await refresh(); } catch (cause) { fail(cause.message); } }
  async function remove(player) { try { await send(`/players/${player.id}`, 'DELETE', null, credentials); await refresh(); } catch (cause) { fail(cause.message); } }
  return <div className="content directory-page"><PageHeading tag="PLAYER REGISTER" title="Players" detail="Squad records and team associations." />{writable && <form className="entry-form" onSubmit={submit}><input required placeholder="First name" value={form.firstName} onChange={(event) => setForm({ ...form, firstName: event.target.value })} /><input required placeholder="Last name" value={form.lastName} onChange={(event) => setForm({ ...form, lastName: event.target.value })} /><select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}><option>BATSMAN</option><option>BOWLER</option><option>ALL_ROUNDER</option><option>WICKET_KEEPER</option></select><select required value={form.teamId} onChange={(event) => setForm({ ...form, teamId: event.target.value })}><option value="">Choose team</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select><button>Add player +</button></form>}<div className="player-list"><div><small>PLAYER</small><small>ROLE</small><small>TEAM</small><span /></div>{players.map((player, index) => <div key={player.id}><strong><i>{String(index + 1).padStart(2, '0')}</i>{player.firstName} {player.lastName}</strong><span>{player.role?.replace('_', ' ')}</span><span>{player.teamName || 'Unassigned'}</span>{writable && <button onClick={() => remove(player)} aria-label={`Delete ${player.firstName}`}>×</button>}</div>)}</div>{!players.length && <Empty title="No player records" detail="Register players and assign them to teams." />}</div>;
}

function Matches({ matches, teams, credentials, refresh, fail, onOpen }) {
  const [form, setForm] = useState({ title: '', teamAId: '', teamBId: '', venue: '', matchType: 'T20', scheduledDate: '' });
  const writable = credentials?.roles?.some((role) => roles.includes(role));
  async function submit(event) { event.preventDefault(); try { await send('/matches', 'POST', { ...form, teamAId: Number(form.teamAId), teamBId: Number(form.teamBId) }, credentials); setForm({ title: '', teamAId: '', teamBId: '', venue: '', matchType: 'T20', scheduledDate: '' }); await refresh(); } catch (cause) { fail(cause.message); } }
  async function remove(match) { try { await send(`/matches/${match.id}`, 'DELETE', null, credentials); await refresh(); } catch (cause) { fail(cause.message); } }
  return <div className="content directory-page"><PageHeading tag="FIXTURES & RESULTS" title="Matches" detail="A season takes shape one fixture at a time." />{writable && <form className="entry-form match-form" onSubmit={submit}><input required placeholder="Fixture title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /><select required value={form.teamAId} onChange={(event) => setForm({ ...form, teamAId: event.target.value })}><option value="">First team</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select><select required value={form.teamBId} onChange={(event) => setForm({ ...form, teamBId: event.target.value })}><option value="">Second team</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select><input required placeholder="Venue" value={form.venue} onChange={(event) => setForm({ ...form, venue: event.target.value })} /><select value={form.matchType} onChange={(event) => setForm({ ...form, matchType: event.target.value })}><option>T20</option><option>ODI</option><option>TEST</option></select><input required type="datetime-local" value={form.scheduledDate} onChange={(event) => setForm({ ...form, scheduledDate: event.target.value })} /><button>Schedule match +</button></form>}<div className="match-list">{matches.map((match) => <div key={match.id}><span className={match.status === 'LIVE' ? 'state live' : 'state'}>{match.status}</span><button onClick={() => onOpen(match)}><b>{match.teamAName} <i>vs</i> {match.teamBName}</b><small>{match.title}</small></button><span>{match.matchType}</span><span>{formatDate(match.scheduledDate)} · {match.venue}</span>{writable && <button onClick={() => remove(match)} aria-label={`Delete ${match.title}`}>×</button>}</div>)}</div>{!matches.length && <Empty title="No fixtures yet" detail="Create a match after registering two teams." />}</div>;
}

function MatchCenter({ match, scores, players, teams, canScore, credentials, refreshScores, fail }) {
  const [form, setForm] = useState({ battingTeamId: '', bowlingTeamId: '', targetRuns: '' });
  const [busy, setBusy] = useState(false);
  const latest = scores.at(-1);
  async function startInnings(event) { event.preventDefault(); setBusy(true); try { await send(`/matches/${match.id}/innings`, 'POST', { battingTeamId: Number(form.battingTeamId), bowlingTeamId: Number(form.bowlingTeamId), inningsNumber: scores.length + 1, ...(form.targetRuns ? { targetRuns: Number(form.targetRuns) } : {}) }, credentials); setForm({ battingTeamId: '', bowlingTeamId: '', targetRuns: '' }); await refreshScores(); } catch (cause) { fail(cause.message); } finally { setBusy(false); } }
  async function record(event) { event.preventDefault(); const formElement = event.currentTarget; const fields = new FormData(formElement); setBusy(true); try { const extraType = fields.get('extraType'); const wicketType = fields.get('wicketType'); await send(`/innings/${latest.id}/deliveries`, 'POST', { overNumber: Math.floor(latest.legalBalls / 6) + 1, ballNumber: latest.legalBalls % 6 + 1, batsmanId: Number(fields.get('batsmanId') || latest.strikerId), nonStrikerId: Number(fields.get('nonStrikerId') || latest.nonStrikerId), bowlerId: Number(fields.get('bowlerId') || latest.currentBowlerId), runs: Number(fields.get('runs')), extraType, extraRuns: Number(fields.get('extraRuns')), ...(wicketType ? { wicketType, dismissedBatsmanId: Number(fields.get('dismissedBatsmanId')) } : {}) }, credentials); formElement.reset(); await refreshScores(); } catch (cause) { fail(cause.message); } finally { setBusy(false); } }
  const batters = players.filter((player) => String(player.teamId) === String(latest?.battingTeamId));
  const bowlers = players.filter((player) => String(player.teamId) === String(latest?.bowlingTeamId));
  return <div className="content match-center"><button className="back-link" onClick={() => window.dispatchEvent(new CustomEvent('cricket:navigate', { detail: 'matches' }))}>← All matches</button><section className="match-banner"><span className="overline">{match.matchType} · {match.status} · {match.venue}</span><h1>{match.teamAName}<i>vs</i>{match.teamBName}</h1><p>{match.title} <span>{formatDate(match.scheduledDate)}</span></p></section><div className="score-panels">{scores.map((score) => <article key={score.id}><div><span className="overline">INNINGS {score.inningsNumber} · {score.status}</span><strong>{score.totalRuns}<small>/{score.wickets}</small></strong></div><h2>{score.battingTeamName}</h2><footer>{score.overs} overs <i>RR {Number(score.runRate || 0).toFixed(2)}</i><span>{score.targetRuns ? `Target ${score.targetRuns}` : score.bowlingTeamName + ' bowling'}</span></footer></article>)}</div>
    {scores.length > 0 && <Scorecard scores={scores} />}
    {canScore && scores.length < 4 && <form className="scorer-form" onSubmit={startInnings}><div><span className="overline">SCORER DESK</span><h2>Start an innings</h2></div><select required value={form.battingTeamId} onChange={(event) => setForm({ ...form, battingTeamId: event.target.value })}><option value="">Batting side</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select><select required value={form.bowlingTeamId} onChange={(event) => setForm({ ...form, bowlingTeamId: event.target.value })}><option value="">Bowling side</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select><input type="number" min="1" placeholder="Target (optional)" value={form.targetRuns} onChange={(event) => setForm({ ...form, targetRuns: event.target.value })} /><button disabled={busy}>Start innings</button></form>}
    {canScore && latest?.status === 'LIVE' && <form className="scorer-form" onSubmit={record}><div><span className="overline">BALL-BY-BALL ENTRY</span><h2>Record delivery <small>{latest.overs}</small></h2></div><select name="batsmanId" required defaultValue={latest.strikerId || ''}><option value="">Striker</option>{batters.map((player) => <option key={player.id} value={player.id}>{player.firstName} {player.lastName}</option>)}</select><select name="nonStrikerId" required defaultValue={latest.nonStrikerId || ''}><option value="">Non-striker</option>{batters.map((player) => <option key={player.id} value={player.id}>{player.firstName} {player.lastName}</option>)}</select><select name="bowlerId" required defaultValue={latest.currentBowlerId || ''}><option value="">Bowler</option>{bowlers.map((player) => <option key={player.id} value={player.id}>{player.firstName} {player.lastName}</option>)}</select><select name="runs">{[0, 1, 2, 3, 4, 6].map((run) => <option key={run} value={run}>{run} runs</option>)}</select><select name="extraType">{['NONE', 'WIDE', 'NO_BALL', 'BYE', 'LEG_BYE', 'PENALTY'].map((extra) => <option key={extra}>{extra}</option>)}</select><input name="extraRuns" type="number" min="0" defaultValue="0" aria-label="Extra runs" /><select name="wicketType"><option value="">No wicket</option>{['BOWLED', 'CAUGHT', 'LBW', 'RUN_OUT', 'STUMPED', 'HIT_WICKET'].map((type) => <option key={type}>{type}</option>)}</select><select name="dismissedBatsmanId" defaultValue={latest.strikerId || ''}><option value="">Dismissed batter</option>{batters.map((player) => <option key={player.id} value={player.id}>{player.firstName} {player.lastName}</option>)}</select><button disabled={busy}>Save delivery</button></form>}
  </div>;
}

function Scorecard({ scores }) {
  return <div className="scorecard-stack">{scores.map((score) => <section key={score.id}><header><div><span className="overline">SCORECARD · INNINGS {score.inningsNumber}</span><h2>{score.battingTeamName}</h2></div><b>{score.totalRuns}/{score.wickets}</b></header><div className="score-table"><div><span>BATTER</span><b>R</b><b>B</b><b>4s</b><b>6s</b><b>SR</b></div>{score.batting?.map((player) => <div key={player.playerId}><span>{player.playerName}{player.dismissed && <small> out</small>}</span><b>{player.runs}</b><span>{player.ballsFaced}</span><span>{player.fours}</span><span>{player.sixes}</span><span>{Number(player.strikeRate).toFixed(2)}</span></div>)}</div><p className="extras-line">EXTRAS <b>{score.extras?.total || 0}</b><span>WD {score.extras?.wides || 0} · NB {score.extras?.noBalls || 0} · B {score.extras?.byes || 0} · LB {score.extras?.legByes || 0} · P {score.extras?.penalties || 0}</span></p><div className="score-table bowl-table"><div><span>BOWLER</span><b>O</b><b>R</b><b>W</b><b>ECON</b></div>{score.bowling?.map((bowler) => <div key={bowler.playerId}><span>{bowler.playerName}</span><span>{bowler.overs}</span><span>{bowler.runsConceded}</span><b>{bowler.wickets}</b><span>{Number(bowler.economy).toFixed(2)}</span></div>)}</div><footer>TOTAL <b>{score.totalRuns}/{score.wickets}</b><span>{score.overs} overs · RR {Number(score.runRate || 0).toFixed(2)}</span></footer></section>)}</div>;
}

function AuthDialog({ close, authenticated, fail }) {
  const [register, setRegister] = useState(false);
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  async function submit(event) { event.preventDefault(); try { if (register) await send('/auth/register', 'POST', form); const login = { username: form.username, password: form.password }; authenticated(await api('/auth/me', { credentials: login }), login); } catch (cause) { fail(cause.message); } }
  return <div className="modal-shade" onMouseDown={(event) => { if (event.target === event.currentTarget) close(); }}><form className="auth-modal" onSubmit={submit}><button className="modal-close" type="button" onClick={close}>×</button><span className="overline">SCORE DESK ACCESS</span><h2>{register ? 'Create viewer account' : 'Welcome back'}</h2>{register && <label>Email<input type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>}<label>Username<input required autoComplete="username" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></label><label>Password<input type="password" required minLength={register ? 12 : 1} autoComplete={register ? 'new-password' : 'current-password'} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label><button className="submit-button">{register ? 'Register viewer' : 'Sign in'}</button><button className="auth-toggle" type="button" onClick={() => setRegister(!register)}>{register ? 'Already registered? Sign in' : 'Need a viewer account? Register'}</button></form></div>;
}

function formatDate(value) {
  if (!value) return 'Date TBC';
  return new Intl.DateTimeFormat('en', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value));
}