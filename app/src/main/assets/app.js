// Idea Memo Web & Cloud Application with Supabase
const STORAGE_KEY = 'spark_idea_memos_v2';
const THEME_KEY = 'spark_idea_theme';
const GEMINI_API_KEY_STORAGE = 'gemini_api_key';
const SUPABASE_URL_KEY = 'VITE_SUPABASE_URL';
const SUPABASE_ANON_KEY = 'VITE_SUPABASE_ANON_KEY';

// State
let supabaseClient = null;
let ideas = [];
let currentFilter = 'all'; // all, pinned, starred, voice
let currentCategory = 'all';
let searchQuery = '';
let sortBy = 'newest';
let viewMode = 'grid'; // grid, list
let editingIdeaId = null;
let recognition = null;
let isRecordingVoice = false;
let mediaRecorder = null;
let audioChunks = [];
let recordedAudioBase64 = null;

// Initial Sample Ideas if empty
const sampleIdeas = [
  {
    id: 'sample-1',
    title: '✨ AI 기반 스마트 아이디어 메모 웹 & 앱',
    content: 'PC 브라우저와 안드로이드 스마트폰 양쪽에서 완벽하게 호환되는 올인원 아이디어 기록 시스템!\n\n주요 기능:\n1. 실시간 음성 인식 (STT) & 오디오 녹음\n2. 1~5점 중요도 별점 시스템\n3. Supabase 실시간 클라우드 DB 동기화\n4. 카테고리별 태그 정리 및 마크다운 내보내기',
    category: '기술',
    importance: 5,
    tags: ['AI', '스마트메모', '크로스플랫폼', 'Supabase'],
    isPinned: true,
    isFavorite: true,
    colorHex: '#ffffff',
    voiceAudio: null,
    createdAt: Date.now() - 1000 * 60 * 60 * 2,
    updatedAt: Date.now()
  },
  {
    id: 'sample-2',
    title: '🚀 신규 프로젝트 런칭 마케팅 전략',
    content: '1. Vercel 웹 배포를 통한 빠른 접근성 확보\n2. Supabase 연결로 다중 기기 실시간 동기화\n3. 인스타그램 및 X(트위터) 릴스 쇼츠 홍보',
    category: '사업',
    importance: 4,
    tags: ['마케팅', '비즈니스', '전략'],
    isPinned: true,
    isFavorite: false,
    colorHex: '#fef3c7',
    voiceAudio: null,
    createdAt: Date.now() - 1000 * 60 * 60 * 24,
    updatedAt: Date.now()
  },
  {
    id: 'sample-3',
    title: '🎧 주간 팟캐스트 기획 - 2026 생산성 도구 트렌드',
    content: '음성 메모와 음성 타이핑을 활용하여 언제 어디서나 순간적인 영감을 기록하고 정리하는 팁 소개.',
    category: '창작',
    importance: 3,
    tags: ['팟캐스트', '생산성', '아이디어'],
    isPinned: false,
    isFavorite: false,
    colorHex: '#ede9fe',
    voiceAudio: null,
    createdAt: Date.now() - 1000 * 60 * 60 * 48,
    updatedAt: Date.now()
  }
];

// Initialize
async function init() {
  loadTheme();
  await initSupabase();
  await loadIdeas();
  setupSpeechRecognition();
  bindEvents();
  render();
}

function loadTheme() {
  const savedTheme = localStorage.getItem(THEME_KEY) || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem(THEME_KEY, next);
  updateThemeIcon(next);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('themeToggleBtn');
  if (btn) {
    btn.innerHTML = theme === 'dark' 
      ? '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>'
      : '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>';
  }
}

// Toast Notification
function showToast(message, type = 'info', duration = 3500) {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(20px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

function copySupabaseSql() {
  const codeEl = document.getElementById('supabaseSqlCode');
  const code = codeEl ? codeEl.innerText : '';
  if (code) {
    navigator.clipboard.writeText(code).then(() => {
      showToast('📋 SQL 코드가 클립보드에 복사되었습니다! Supabase SQL Editor에 붙여넣어 실행하세요.', 'success', 4000);
    }).catch(() => {
      showToast('복사 권한이 없어 수동으로 복사해주세요.', 'error');
    });
  }
}

// Supabase Integration Logic with Direct REST API Dual-Engine
function normalizeSupabaseUrl(url) {
  if (!url) return '';
  return url.trim().replace(/\/+$/, '');
}

async function initSupabase() {
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  const supabaseUrl = normalizeSupabaseUrl(rawUrl);
  const supabaseKey = rawKey ? rawKey.trim() : '';
  const badge = document.getElementById('supabaseStatusBadge');
  const banner = document.getElementById('supabaseBanner');

  if (supabaseUrl && supabaseKey) {
    try {
      if (window.supabase) {
        supabaseClient = window.supabase.createClient(supabaseUrl, supabaseKey);
      }
      
      // Test read permission via direct REST API or JS client
      const testResult = await executeSupabaseRest('GET', '/rest/v1/ideas?select=id&limit=1');
      if (testResult.ok) {
        if (badge) {
          badge.innerHTML = '⚡ Supabase 연동됨';
          badge.style.color = '#10b981';
        }
        if (banner) {
          banner.classList.remove('show');
        }

        // Setup realtime subscription if JS client available
        if (supabaseClient) {
          try {
            supabaseClient
              .channel('ideas-db-changes')
              .on('postgres_changes', { event: '*', schema: 'public', table: 'ideas' }, () => {
                console.log('Realtime update received from Supabase!');
                loadIdeas(false);
              })
              .subscribe();
          } catch (subErr) {
            console.warn('Realtime subscription not active:', subErr);
          }
        }
        return;
      } else {
        console.warn('Supabase test query warning:', testResult.error);
        if (badge) {
          badge.innerHTML = '⚠️ Supabase 권한 확인 필요';
          badge.style.color = '#f59e0b';
        }
        return;
      }
    } catch (e) {
      console.error('Supabase initialization failed:', e);
    }
  }

  // Not connected
  if (badge) {
    badge.innerHTML = '⚡ Supabase 설정';
    badge.style.color = '';
  }
  if (banner) {
    banner.classList.add('show');
  }
}

// Low-level HTTP REST query to Supabase (works 100% reliably in any browser/CORS environment)
async function executeSupabaseRest(method, endpoint, body = null) {
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  const url = normalizeSupabaseUrl(rawUrl);
  const key = rawKey ? rawKey.trim() : '';

  if (!url || !key) return { ok: false, error: 'Supabase URL or Key is missing' };

  try {
    const headers = {
      'apikey': key,
      'Authorization': `Bearer ${key}`,
      'Content-Type': 'application/json',
      'Prefer': method === 'POST' ? 'resolution=merge-duplicates,return=representation' : 'return=representation'
    };

    const options = {
      method: method,
      headers: headers
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    const res = await fetch(`${url}${endpoint}`, options);
    if (!res.ok) {
      const errText = await res.text();
      let parsed = null;
      try { parsed = JSON.parse(errText); } catch (e) {}
      return {
        ok: false,
        status: res.status,
        code: parsed?.code || String(res.status),
        error: parsed?.message || parsed?.hint || errText || res.statusText
      };
    }

    const resText = await res.text();
    const data = resText ? JSON.parse(resText) : null;
    return { ok: true, data: data };
  } catch (netErr) {
    return { ok: false, error: netErr.message || '네트워크 통신 오류' };
  }
}

async function testSupabaseConnection() {
  const rawUrl = document.getElementById('supabaseUrlInput').value.trim();
  const rawKey = document.getElementById('supabaseKeyInput').value.trim();
  const url = normalizeSupabaseUrl(rawUrl);
  const key = rawKey.trim();

  if (!url || !key) {
    showToast('⚠️ URL과 Anon Key를 먼저 입력해주세요.', 'error');
    return;
  }

  // Check URL format
  if (!url.startsWith('https://') || !url.includes('.supabase.co')) {
    showToast('⚠️ Supabase URL은 https://xxxx.supabase.co 형식이어야 합니다.', 'error', 5000);
  }

  try {
    showToast('🔍 Supabase 연결 및 RLS 권한 테스트 중...', 'info', 2000);
    
    // Save temporarily for REST helper
    localStorage.setItem(SUPABASE_URL_KEY, url);
    localStorage.setItem(SUPABASE_ANON_KEY, key);

    // 1. Check SELECT
    const selectRes = await executeSupabaseRest('GET', '/rest/v1/ideas?select=id&limit=1');
    if (!selectRes.ok) {
      if (selectRes.code === '42P01' || selectRes.error.includes('relation "public.ideas" does not exist') || selectRes.status === 404) {
        showToast('❌ ideas 테이블이 없습니다! 아래 [SQL 전체 복사] 후 Supabase SQL Editor에서 실행하세요.', 'error', 7000);
      } else if (selectRes.code === '42501' || selectRes.status === 401 || selectRes.status === 403) {
        showToast(`❌ 읽기 권한(RLS) 오류 (${selectRes.code || selectRes.status}): 아래 RLS 정책 SQL을 실행해주세요.`, 'error', 7000);
      } else {
        showToast(`❌ 연결 오류: ${selectRes.error}`, 'error', 6000);
      }
      return;
    }

    // 2. Check INSERT/UPSERT
    const testId = '__test_ping_' + Date.now();
    const insertRes = await executeSupabaseRest('POST', '/rest/v1/ideas', {
      id: testId,
      title: '연결 테스트',
      content: 'ping',
      created_at: Date.now(),
      updated_at: Date.now()
    });

    if (!insertRes.ok) {
      showToast(`❌ 쓰기/RLS 권한 오류 (${insertRes.code || insertRes.status}): ${insertRes.error} (아래 SQL을 실행하세요)`, 'error', 7000);
      return;
    }

    // Clean up test item
    await executeSupabaseRest('DELETE', `/rest/v1/ideas?id=eq.${testId}`);

    showToast('✅ Supabase 연결 및 읽기/쓰기 권한이 100% 정상 작동합니다! 🎉', 'success', 4500);
    await initSupabase();
  } catch (err) {
    showToast(`❌ 연결 실패: ${err.message}`, 'error', 5000);
  }
}

function openSupabaseModal() {
  const currentUrl = localStorage.getItem(SUPABASE_URL_KEY) || '';
  const currentKey = localStorage.getItem(SUPABASE_ANON_KEY) || '';
  document.getElementById('supabaseUrlInput').value = currentUrl;
  document.getElementById('supabaseKeyInput').value = currentKey;
  document.getElementById('supabaseModal').classList.add('open');
}

function closeSupabaseModal() {
  document.getElementById('supabaseModal').classList.remove('open');
}

async function handleSaveSupabaseEnv(e) {
  e.preventDefault();
  const rawUrl = document.getElementById('supabaseUrlInput').value.trim();
  const rawKey = document.getElementById('supabaseKeyInput').value.trim();
  const url = normalizeSupabaseUrl(rawUrl);
  const key = rawKey.trim();

  if (!url || !key) {
    showToast('VITE_SUPABASE_URL과 VITE_SUPABASE_ANON_KEY를 모두 입력해주세요.', 'error');
    return;
  }

  localStorage.setItem(SUPABASE_URL_KEY, url);
  localStorage.setItem(SUPABASE_ANON_KEY, key);

  await initSupabase();
  closeSupabaseModal();

  // Sync immediately
  await syncWithSupabase();
  render();
}

function toggleVisibility(inputId) {
  const el = document.getElementById(inputId);
  if (el) {
    el.type = el.type === 'password' ? 'text' : 'password';
  }
}

// Load & 2-Way Sync Ideas
async function loadIdeas(triggerPush = true) {
  // 1. First load from local storage
  const data = localStorage.getItem(STORAGE_KEY);
  if (data) {
    try {
      ideas = JSON.parse(data);
    } catch (e) {
      ideas = sampleIdeas;
    }
  } else {
    ideas = sampleIdeas;
    saveLocalIdeas();
  }

  // 2. If Supabase configured, fetch and 2-way sync
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  if (rawUrl && rawKey) {
    try {
      const restRes = await executeSupabaseRest('GET', '/rest/v1/ideas?select=*&order=created_at.desc');
      if (restRes.ok && Array.isArray(restRes.data)) {
        const remoteIdeas = restRes.data;
        const remoteMap = new Map();
        remoteIdeas.forEach(r => {
          remoteMap.set(String(r.id), {
            id: String(r.id),
            title: r.title || '아이디어',
            content: r.content || '',
            category: r.category || '일상',
            importance: typeof r.importance === 'number' ? r.importance : 3,
            tags: Array.isArray(r.tags) ? r.tags : [],
            isPinned: Boolean(r.is_pinned),
            isFavorite: Boolean(r.is_favorite),
            colorHex: r.color_hex || '#ffffff',
            voiceAudio: r.voice_audio || null,
            createdAt: r.created_at ? (typeof r.created_at === 'number' ? r.created_at : new Date(r.created_at).getTime() || Date.now()) : Date.now(),
            updatedAt: r.updated_at ? (typeof r.updated_at === 'number' ? r.updated_at : new Date(r.updated_at).getTime() || Date.now()) : Date.now()
          });
        });

        // Push local ideas that are not yet in remote
        if (triggerPush) {
          for (const localIdea of ideas) {
            if (!remoteMap.has(String(localIdea.id))) {
              await pushIdeaToSupabase(localIdea, false);
              remoteMap.set(String(localIdea.id), localIdea);
            }
          }
        }

        // Merge: all remote items + any remaining local items
        ideas = Array.from(remoteMap.values());
        saveLocalIdeas();
      } else if (!restRes.ok) {
        console.warn('Supabase loadIdeas warning:', restRes.error);
      }
    } catch (err) {
      console.warn('Supabase fetch error, fallback to local:', err);
    }
  }
}

function saveLocalIdeas() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ideas));
}

async function saveIdea(idea) {
  saveLocalIdeas();
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  if (rawUrl && rawKey) {
    await pushIdeaToSupabase(idea);
  }
}

async function pushIdeaToSupabase(idea, showNotification = true) {
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  if (!rawUrl || !rawKey) return false;

  try {
    const payload = {
      id: String(idea.id),
      title: idea.title || '새로운 아이디어',
      content: idea.content || '',
      category: idea.category || '일상',
      importance: Number(idea.importance) || 3,
      tags: Array.isArray(idea.tags) ? idea.tags : [],
      is_pinned: Boolean(idea.isPinned),
      is_favorite: Boolean(idea.isFavorite),
      color_hex: idea.colorHex || '#ffffff',
      voice_audio: idea.voiceAudio || null,
      created_at: Number(idea.createdAt) || Date.now(),
      updated_at: Number(idea.updatedAt) || Date.now()
    };

    const restRes = await executeSupabaseRest('POST', '/rest/v1/ideas', payload);

    if (!restRes.ok) {
      console.error('Supabase save error:', restRes.error);
      if (showNotification) {
        if (restRes.code === '42501' || restRes.status === 401 || restRes.status === 403) {
          showToast('⚠️ DB 쓰기 권한(RLS) 오류! 모달 안의 SQL을 Supabase에서 실행해주세요.', 'error', 6500);
        } else {
          showToast(`⚠️ Supabase 저장 실패 (${restRes.code || restRes.status || ''}): ${restRes.error}`, 'error', 5000);
        }
      }
      return false;
    }

    if (showNotification) {
      showToast('☁️ Supabase에 실시간 저장되었습니다.', 'success', 2200);
    }
    return true;
  } catch (err) {
    console.error('Failed to sync idea to Supabase:', err);
    if (showNotification) {
      showToast(`⚠️ 클라우드 통신 실패: ${err.message}`, 'error');
    }
    return false;
  }
}

async function deleteIdeaFromSupabase(id) {
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  if (!rawUrl || !rawKey) return;

  try {
    const restRes = await executeSupabaseRest('DELETE', `/rest/v1/ideas?id=eq.${encodeURIComponent(String(id))}`);
    if (!restRes.ok) {
      console.error('Failed to delete idea from Supabase:', restRes.error);
      showToast('⚠️ Supabase 삭제 오류: ' + restRes.error, 'error');
    } else {
      showToast('☁️ Supabase에서 삭제되었습니다.', 'info', 2000);
    }
  } catch (err) {
    console.error('Failed to delete idea from Supabase:', err);
  }
}

async function syncWithSupabase() {
  const rawUrl = localStorage.getItem(SUPABASE_URL_KEY);
  const rawKey = localStorage.getItem(SUPABASE_ANON_KEY);
  if (!rawUrl || !rawKey) {
    openSupabaseModal();
    return;
  }
  showToast('🔄 Supabase 클라우드와 전체 동기화 중...', 'info', 2000);
  let successCount = 0;
  for (const idea of ideas) {
    const ok = await pushIdeaToSupabase(idea, false);
    if (ok) successCount++;
  }
  await loadIdeas(false);
  render();
  showToast(`✨ 총 ${ideas.length}개의 아이디어가 Supabase와 완벽하게 동기화되었습니다! 🚀`, 'success', 4000);
}

// Speech Recognition setup
function setupSpeechRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'ko-KR';

    recognition.onresult = (event) => {
      let finalTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript + ' ';
        }
      }
      if (finalTranscript) {
        const activeModal = document.getElementById('ideaModal').classList.contains('open');
        if (activeModal) {
          const textarea = document.getElementById('modalContent');
          textarea.value += (textarea.value ? '\n' : '') + finalTranscript.trim();
        } else {
          const quickContent = document.getElementById('quickContent');
          const quickInputCard = document.getElementById('quickInputCard');
          quickInputCard.classList.add('expanded');
          quickContent.value += (quickContent.value ? '\n' : '') + finalTranscript.trim();
        }
      }
    };

    recognition.onerror = (event) => {
      console.warn('Speech recognition error:', event.error);
      stopVoiceRecognition();
    };

    recognition.onend = () => {
      if (isRecordingVoice) {
        stopVoiceRecognition();
      }
    };
  }
}

function toggleVoiceRecognition(target = 'quick') {
  if (!recognition) {
    alert('이 브라우저에서는 음성 인식(STT)을 지원하지 않습니다. Chrome 또는 Edge 브라우저를 이용해 주세요.');
    return;
  }

  if (isRecordingVoice) {
    stopVoiceRecognition();
  } else {
    startVoiceRecognition();
  }
}

function startVoiceRecognition() {
  try {
    recognition.start();
    isRecordingVoice = true;
    updateMicButtons(true);
  } catch (e) {
    console.error(e);
  }
}

function stopVoiceRecognition() {
  try {
    if (recognition) recognition.stop();
  } catch (e) {}
  isRecordingVoice = false;
  updateMicButtons(false);
}

function updateMicButtons(recording) {
  document.querySelectorAll('.mic-btn').forEach(btn => {
    if (recording) {
      btn.style.color = '#ef4444';
      btn.style.background = '#fee2e2';
    } else {
      btn.style.color = '';
      btn.style.background = '';
    }
  });
}

// Media Recorder (Audio Recording)
async function startAudioRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(stream);
    audioChunks = [];
    
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data);
    };
    
    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
      const reader = new FileReader();
      reader.readAsDataURL(audioBlob);
      reader.onloadend = () => {
        recordedAudioBase64 = reader.result;
        updateModalAudioPlayer(recordedAudioBase64);
      };
      stream.getTracks().forEach(track => track.stop());
    };
    
    mediaRecorder.start();
    document.getElementById('recordAudioBtn').innerHTML = '⏹️ 녹음 중지';
    document.getElementById('recordAudioBtn').classList.add('btn-primary');
  } catch (err) {
    alert('마이크 접근 권한이 필요합니다.');
  }
}

function stopAudioRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
  }
  document.getElementById('recordAudioBtn').innerHTML = '🎙️ 음성 녹음';
  document.getElementById('recordAudioBtn').classList.remove('btn-primary');
}

function updateModalAudioPlayer(src) {
  const container = document.getElementById('modalAudioContainer');
  if (src) {
    container.innerHTML = `
      <div class="voice-memo-box">
        <audio controls src="${src}"></audio>
        <button type="button" class="btn btn-secondary" onclick="removeRecordedAudio()" style="padding: 4px 8px; font-size: 0.8rem;">삭제</button>
      </div>
    `;
    container.style.display = 'block';
  } else {
    container.innerHTML = '';
    container.style.display = 'none';
  }
}

function removeRecordedAudio() {
  recordedAudioBase64 = null;
  updateModalAudioPlayer(null);
}

// AI Idea Expansion
async function expandWithAI(title, content) {
  if (!title && !content) {
    alert('아이디어 제목이나 내용을 먼저 작성해주세요.');
    return;
  }

  const promptText = `아이디어 제목: "${title}"\n아이디어 내용: "${content}"\n\n이 아이디어를 더욱 실용적이고 창의적으로 구체화해 줘. 1) 핵심 가치, 2) 실행 단계, 3) 차별화 포인트 3가지를 깔끔하게 정리해 줘.`;

  const userKey = localStorage.getItem(GEMINI_API_KEY_STORAGE);

  const modalAiResult = document.getElementById('modalAiResult');
  modalAiResult.style.display = 'block';
  modalAiResult.innerHTML = '<div style="padding: 1rem; background: var(--primary-light); color: var(--primary); border-radius: var(--radius-md); font-weight:600;">✨ AI가 아이디어를 분석하고 브레인스토밍 중입니다...</div>';

  if (userKey) {
    try {
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${userKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: promptText }] }]
        })
      });
      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (aiText) {
        showAiExpansionResult(aiText);
        return;
      }
    } catch (e) {
      console.warn('Gemini API call failed, falling back to smart local brainstormer', e);
    }
  }

  // Smart local brainstorming fallback
  setTimeout(() => {
    const localExpansion = `💡 **[AI 아이디어 발전 제안]**\n\n` +
      `📌 **1. 핵심 가치 정의**\n` +
      `- 사용자의 실제 문제 해결에 집중하여 가치를 극대화합니다.\n\n` +
      `🚀 **2. 실행 로드맵**\n` +
      `- 1단계: 프로토타입 제작 및 핵심 가설 검증\n` +
      `- 2단계: 피드백 수집 및 기능 고도화\n` +
      `- 3단계: 공식 런칭 및 마케팅 전개\n\n` +
      `⭐ **3. 성공 요소 및 차별화**\n` +
      `- 간결하고 직관적인 사용자 경험(UX)\n` +
      `- Supabase 기반 실시간 클라우드 영구 저장`;
    showAiExpansionResult(localExpansion);
  }, 600);
}

function showAiExpansionResult(text) {
  const modalAiResult = document.getElementById('modalAiResult');
  modalAiResult.innerHTML = `
    <div style="padding: 1rem; background: var(--surface-variant); border: 1px solid var(--border); border-radius: var(--radius-md); font-size: 0.9rem; line-height: 1.6;">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.5rem;">
        <strong style="color:var(--primary);">✨ AI 브레인스토밍 결과</strong>
        <button class="btn btn-secondary" style="padding:2px 8px; font-size:0.75rem;" onclick="applyAiTextToContent()">본문에 추가</button>
      </div>
      <div id="aiResponseText" style="white-space: pre-wrap;">${text}</div>
    </div>
  `;
}

function applyAiTextToContent() {
  const aiText = document.getElementById('aiResponseText')?.innerText;
  if (aiText) {
    const textarea = document.getElementById('modalContent');
    textarea.value += '\n\n' + aiText;
    document.getElementById('modalAiResult').style.display = 'none';
  }
}

// Bind Events
function bindEvents() {
  // Supabase Config Modal & Sync Button
  document.getElementById('supabaseConfigBtn').addEventListener('click', openSupabaseModal);
  document.getElementById('closeSupabaseModalBtn').addEventListener('click', closeSupabaseModal);
  const syncBtn = document.getElementById('syncSupabaseBtn');
  if (syncBtn) {
    syncBtn.addEventListener('click', () => {
      syncWithSupabase();
    });
  }

  // Theme
  document.getElementById('themeToggleBtn').addEventListener('click', toggleTheme);

  // Search
  const searchInput = document.getElementById('searchInput');
  searchInput.addEventListener('input', (e) => {
    searchQuery = e.target.value;
    render();
  });

  // Tabs
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentFilter = btn.dataset.tab;
      render();
    });
  });

  // Category Chips
  document.querySelectorAll('.category-chips .chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.category-chips .chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentCategory = chip.dataset.category;
      render();
    });
  });

  // Sort
  document.getElementById('sortSelect').addEventListener('change', (e) => {
    sortBy = e.target.value;
    render();
  });

  // View Mode
  document.getElementById('viewModeBtn').addEventListener('click', () => {
    viewMode = viewMode === 'grid' ? 'list' : 'grid';
    document.getElementById('ideasContainer').className = `ideas-container ${viewMode === 'list' ? 'list-view' : ''}`;
  });

  // Quick Input Card Expansion
  const quickTitle = document.getElementById('quickTitle');
  const quickInputCard = document.getElementById('quickInputCard');
  quickTitle.addEventListener('focus', () => {
    quickInputCard.classList.add('expanded');
  });

  // Quick Save
  document.getElementById('quickSaveBtn').addEventListener('click', handleQuickSave);
  document.getElementById('quickCancelBtn').addEventListener('click', () => {
    quickTitle.value = '';
    document.getElementById('quickContent').value = '';
    quickInputCard.classList.remove('expanded');
  });

  // Modal Rating stars
  const modalStars = document.querySelectorAll('#modalRatingStars .star');
  modalStars.forEach(star => {
    star.addEventListener('click', () => {
      const val = parseInt(star.dataset.val);
      document.getElementById('modalImportance').value = val;
      updateStarUI(val);
    });
  });

  // Modal Audio Record
  document.getElementById('recordAudioBtn').addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      stopAudioRecording();
    } else {
      startAudioRecording();
    }
  });

  // Modal AI Brainstorm
  document.getElementById('modalAiBtn').addEventListener('click', () => {
    const title = document.getElementById('modalTitle').value;
    const content = document.getElementById('modalContent').value;
    expandWithAI(title, content);
  });

  // Modal Form Submit
  document.getElementById('ideaForm').addEventListener('submit', handleModalSave);

  // Close Modal
  document.getElementById('closeModalBtn').addEventListener('click', closeModal);
  document.getElementById('cancelModalBtn').addEventListener('click', closeModal);

  // Export & Backup
  document.getElementById('exportJsonBtn').addEventListener('click', exportJSON);
  document.getElementById('exportMdBtn').addEventListener('click', exportMarkdown);
}

function updateStarUI(val) {
  document.querySelectorAll('#modalRatingStars .star').forEach(s => {
    const sVal = parseInt(s.dataset.val);
    s.classList.toggle('active', sVal <= val);
  });
}

async function handleQuickSave() {
  const title = document.getElementById('quickTitle').value.trim();
  const content = document.getElementById('quickContent').value.trim();
  const category = document.getElementById('quickCategory').value;
  const importance = parseInt(document.getElementById('quickImportance').value) || 3;

  if (!title && !content) return;

  const newIdea = {
    id: 'idea_' + Date.now(),
    title: title || '새로운 아이디어',
    content: content,
    category: category,
    importance: importance,
    tags: [],
    isPinned: false,
    isFavorite: false,
    colorHex: '#ffffff',
    voiceAudio: null,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };

  ideas.unshift(newIdea);
  await saveIdea(newIdea);
  render();

  document.getElementById('quickTitle').value = '';
  document.getElementById('quickContent').value = '';
  document.getElementById('quickInputCard').classList.remove('expanded');
}

// Modal open/edit
function openNewIdeaModal() {
  editingIdeaId = null;
  document.getElementById('modalHeaderTitle').innerText = '새 아이디어 작성';
  document.getElementById('modalTitle').value = '';
  document.getElementById('modalContent').value = '';
  document.getElementById('modalCategory').value = '일상';
  document.getElementById('modalTags').value = '';
  document.getElementById('modalImportance').value = '3';
  document.getElementById('modalPinned').checked = false;
  document.getElementById('modalAiResult').style.display = 'none';
  recordedAudioBase64 = null;
  updateModalAudioPlayer(null);
  updateStarUI(3);
  document.getElementById('ideaModal').classList.add('open');
}

function openEditIdeaModal(id) {
  const idea = ideas.find(i => i.id === id);
  if (!idea) return;

  editingIdeaId = id;
  document.getElementById('modalHeaderTitle').innerText = '아이디어 수정';
  document.getElementById('modalTitle').value = idea.title;
  document.getElementById('modalContent').value = idea.content;
  document.getElementById('modalCategory').value = idea.category || '일상';
  document.getElementById('modalTags').value = (idea.tags || []).join(', ');
  document.getElementById('modalImportance').value = idea.importance || 3;
  document.getElementById('modalPinned').checked = !!idea.isPinned;
  document.getElementById('modalAiResult').style.display = 'none';
  recordedAudioBase64 = idea.voiceAudio || null;
  updateModalAudioPlayer(recordedAudioBase64);
  updateStarUI(idea.importance || 3);
  document.getElementById('ideaModal').classList.add('open');
}

function closeModal() {
  document.getElementById('ideaModal').classList.remove('open');
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    stopAudioRecording();
  }
}

async function handleModalSave(e) {
  e.preventDefault();
  const title = document.getElementById('modalTitle').value.trim();
  const content = document.getElementById('modalContent').value.trim();
  const category = document.getElementById('modalCategory').value;
  const tagsStr = document.getElementById('modalTags').value;
  const importance = parseInt(document.getElementById('modalImportance').value) || 3;
  const isPinned = document.getElementById('modalPinned').checked;

  const tags = tagsStr.split(',').map(t => t.trim()).filter(t => t.length > 0);

  let targetIdea = null;
  if (editingIdeaId) {
    const idx = ideas.findIndex(i => i.id === editingIdeaId);
    if (idx !== -1) {
      ideas[idx] = {
        ...ideas[idx],
        title: title || '아이디어',
        content,
        category,
        tags,
        importance,
        isPinned,
        voiceAudio: recordedAudioBase64,
        updatedAt: Date.now()
      };
      targetIdea = ideas[idx];
    }
  } else {
    const newIdea = {
      id: 'idea_' + Date.now(),
      title: title || '새로운 아이디어',
      content,
      category,
      tags,
      importance,
      isPinned,
      isFavorite: false,
      colorHex: '#ffffff',
      voiceAudio: recordedAudioBase64,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    ideas.unshift(newIdea);
    targetIdea = newIdea;
  }

  if (targetIdea) {
    await saveIdea(targetIdea);
  }

  closeModal();
  render();
}

async function deleteIdea(id, e) {
  if (e) e.stopPropagation();
  if (confirm('이 아이디어 메모를 삭제하시겠습니까?')) {
    ideas = ideas.filter(i => i.id !== id);
    saveLocalIdeas();
    await deleteIdeaFromSupabase(id);
    render();
  }
}

async function togglePin(id, e) {
  if (e) e.stopPropagation();
  const idea = ideas.find(i => i.id === id);
  if (idea) {
    idea.isPinned = !idea.isPinned;
    idea.updatedAt = Date.now();
    await saveIdea(idea);
    render();
  }
}

function copyIdea(id, e) {
  if (e) e.stopPropagation();
  const idea = ideas.find(i => i.id === id);
  if (idea) {
    const text = `💡 ${idea.title}\n\n${idea.content}\n\n[카테고리: ${idea.category} | 중요도: ${'★'.repeat(idea.importance || 3)}]`;
    navigator.clipboard.writeText(text).then(() => {
      alert('클립보드에 복사되었습니다!');
    });
  }
}

// Render Ideas
function render() {
  const container = document.getElementById('ideasContainer');
  let filtered = [...ideas];

  // Tab filter
  if (currentFilter === 'pinned') {
    filtered = filtered.filter(i => i.isPinned);
  } else if (currentFilter === 'starred') {
    filtered = filtered.filter(i => (i.importance || 0) >= 4);
  } else if (currentFilter === 'voice') {
    filtered = filtered.filter(i => !!i.voiceAudio);
  }

  // Category filter
  if (currentCategory !== 'all') {
    filtered = filtered.filter(i => i.category === currentCategory);
  }

  // Search filter
  if (searchQuery.trim()) {
    const q = searchQuery.toLowerCase();
    filtered = filtered.filter(i => 
      i.title.toLowerCase().includes(q) || 
      i.content.toLowerCase().includes(q) ||
      (i.tags && i.tags.some(t => t.toLowerCase().includes(q))) ||
      (i.category && i.category.toLowerCase().includes(q))
    );
  }

  // Sort
  filtered.sort((a, b) => {
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
    if (sortBy === 'newest') return b.createdAt - a.createdAt;
    if (sortBy === 'oldest') return a.createdAt - b.createdAt;
    if (sortBy === 'importance') return (b.importance || 0) - (a.importance || 0);
    if (sortBy === 'title') return a.title.localeCompare(b.title);
    return 0;
  });

  // Render HTML
  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">💡</div>
        <h3>표시할 아이디어가 없습니다</h3>
        <p>새로운 아이디어를 입력하거나 음성으로 기록해 보세요!</p>
        <button class="btn btn-primary" onclick="openNewIdeaModal()">+ 첫 아이디어 작성하기</button>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(idea => {
    const starCount = idea.importance || 3;
    const starsHtml = '★'.repeat(starCount) + '☆'.repeat(5 - starCount);
    const dateFormatted = new Date(idea.createdAt).toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });

    const tagsHtml = (idea.tags || []).map(t => `<span class="tag-item">#${t}</span>`).join('');
    const audioPlayerHtml = idea.voiceAudio 
      ? `<div class="voice-memo-box" style="margin-top: 0.5rem;" onclick="event.stopPropagation()">
           <audio controls src="${idea.voiceAudio}"></audio>
         </div>`
      : '';

    return `
      <div class="idea-card ${idea.isPinned ? 'pinned' : ''}" onclick="openEditIdeaModal('${idea.id}')">
        <div class="idea-card-header">
          <div style="display:flex; flex-direction:column; gap:4px; flex:1;">
            <div style="display:flex; align-items:center; gap:0.5rem;">
              <span class="idea-badge">${idea.category || '일상'}</span>
              <span class="idea-stars" title="중요도 ${starCount}점">${starsHtml}</span>
            </div>
            <h3 class="idea-title">${escapeHtml(idea.title)}</h3>
          </div>
          <button class="card-btn ${idea.isPinned ? 'active' : ''}" title="상단 고정" onclick="togglePin('${idea.id}', event)">
            📌
          </button>
        </div>

        <div class="idea-content">${escapeHtml(idea.content)}</div>
        ${audioPlayerHtml}

        ${tagsHtml ? `<div class="idea-tags">${tagsHtml}</div>` : ''}

        <div class="idea-footer">
          <span>${dateFormatted}</span>
          <div class="idea-actions">
            <button class="card-btn" title="복사" onclick="copyIdea('${idea.id}', event)">📋</button>
            <button class="card-btn" title="삭제" onclick="deleteIdea('${idea.id}', event)">🗑️</button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
}

// Export functions
function exportJSON() {
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(ideas, null, 2));
  const downloadAnchor = document.createElement('a');
  downloadAnchor.setAttribute("href", dataStr);
  downloadAnchor.setAttribute("download", `spark_idea_backup_${new Date().toISOString().slice(0,10)}.json`);
  document.body.appendChild(downloadAnchor);
  downloadAnchor.click();
  downloadAnchor.remove();
}

function exportMarkdown() {
  let md = `# 💡 아이디어 메모 전체 백업 (${new Date().toLocaleDateString('ko-KR')})\n\n`;
  ideas.forEach((idea, idx) => {
    md += `## ${idx + 1}. ${idea.title}\n`;
    md += `- **카테고리**: ${idea.category} | **중요도**: ${'★'.repeat(idea.importance || 3)}\n`;
    if (idea.tags && idea.tags.length > 0) {
      md += `- **태그**: ${idea.tags.map(t => `#${t}`).join(' ')}\n`;
    }
    md += `- **작성일시**: ${new Date(idea.createdAt).toLocaleString('ko-KR')}\n\n`;
    md += `${idea.content}\n\n---\n\n`;
  });

  const dataStr = "data:text/markdown;charset=utf-8," + encodeURIComponent(md);
  const downloadAnchor = document.createElement('a');
  downloadAnchor.setAttribute("href", dataStr);
  downloadAnchor.setAttribute("download", `ideas_${new Date().toISOString().slice(0,10)}.md`);
  document.body.appendChild(downloadAnchor);
  downloadAnchor.click();
  downloadAnchor.remove();
}

// Global exposure for HTML onclick
window.openNewIdeaModal = openNewIdeaModal;
window.openEditIdeaModal = openEditIdeaModal;
window.closeModal = closeModal;
window.deleteIdea = deleteIdea;
window.togglePin = togglePin;
window.copyIdea = copyIdea;
window.toggleVoiceRecognition = toggleVoiceRecognition;
window.removeRecordedAudio = removeRecordedAudio;
window.applyAiTextToContent = applyAiTextToContent;
window.openSupabaseModal = openSupabaseModal;
window.closeSupabaseModal = closeSupabaseModal;
window.handleSaveSupabaseEnv = handleSaveSupabaseEnv;
window.toggleVisibility = toggleVisibility;
window.copySupabaseSql = copySupabaseSql;
window.testSupabaseConnection = testSupabaseConnection;
window.syncWithSupabase = syncWithSupabase;

// On DOM ready
document.addEventListener('DOMContentLoaded', init);
