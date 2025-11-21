// api.js

// 이 함수는 main.js에서 정의될 것입니다.
// fetchWithAuth가 이 함수에 의존한다는 것을 명확히 하기 위해 여기에 선언합니다.
let showLoginView;

// main.js에서 로그인 뷰 핸들러를 설정하는 함수
function setLoginViewHandler(handler) {
    showLoginView = handler;
}

/**
 * Authorization 헤더를 자동으로 추가하고 401 Unauthorized 응답을 처리하여
 * 사용자를 로그아웃시키는 fetch API 래퍼 함수입니다.
 * @param {string} url - 가져올 URL.
 * @param {object} options - fetch에 전달할 옵션.
 * @returns {Promise<Response>} - fetch 응답으로 확인되는 프로미스.
 */
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('invaders_token'); // Corrected key

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers,
    });

    if (response.status === 401) {
        console.error("인증 실패 또는 토큰 만료. 로그아웃합니다.");
        
        localStorage.removeItem('invaders_token'); // Corrected key
        localStorage.removeItem('invaders_username'); // Corrected key
        localStorage.removeItem('invaders_userId'); // Added to clear userId

        // Toast will be shown by main.js if available
        if (typeof toastWarning === 'function') {
            toastWarning("세션이 만료되었습니다. 다시 로그인해주세요.");
        } else {
            alert("세션이 만료되었습니다. 다시 로그인해주세요.");
        }

        // main.js에 의해 showLoginView가 설정되는 것에 의존합니다.
        if (typeof showLoginView === 'function') {
            showLoginView();
        } else {
            // 핸들러가 설정되지 않은 경우의 대체 동작
            window.location.reload();
        }

        // 프로미스 체인을 중지시키기 위해 에러 발생
        throw new Error('Unauthorized');
    }

    return response;
}