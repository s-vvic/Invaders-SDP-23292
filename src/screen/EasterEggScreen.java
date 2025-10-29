package screen;

import java.awt.event.KeyEvent;

public class EasterEggScreen extends Screen {

    // --- (1) 수정: 메뉴 상태를 위한 변수 ---
    /** 메뉴 옵션 *이름* 목록 */
    private String[] menuOptions;
    /** 메뉴 옵션 *상태* (on/off) 목록 (boolean 배열로 변경) */
    private boolean[] optionStates; // <<<< 새로 추가
    /** 현재 선택된 메뉴 옵션의 인덱스 */
    private int selectedOption;
    // ------------------------------------

    public EasterEggScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
        this.returnCode = 1;
        this.inputDelay.setMilliseconds(150);

        // --- (2) 수정: 메뉴 옵션 초기화 ---
        this.menuOptions = new String[] {
            "Invincibility",   // "on/off" 텍스트 제거
            "Infinite Lives", // "on/off" 텍스트 제거
            "Max Score"        // "on/off" 텍스트 제거
        };

        // 'optionStates' 배열을 메뉴 개수만큼 생성 (기본값은 모두 false/off)
        this.optionStates = new boolean[this.menuOptions.length]; // <<<< 새로 추가
        
        this.selectedOption = 0; 
    }

    public final int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * (3) update() 메소드
     * (이전과 동일하며, ENTER 키 부분만 확인하세요)
     */
    protected final void update() {
        super.update();
        draw(); 

        // 입력 딜레이 확인 후 키 입력 처리
        if (this.inputDelay.checkFinished()) {
            
            if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
                // 위쪽 키: 인덱스 감소
                this.selectedOption--;
                if (this.selectedOption < 0) {
                    this.selectedOption = this.menuOptions.length - 1;
                }
                this.inputDelay.reset(); // <<< (2) 이 줄을 다시 추가합니다.
                                         
            } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
                // 아래쪽 키: 인덱스 증가
                this.selectedOption++;
                if (this.selectedOption >= this.menuOptions.length) {
                    this.selectedOption = 0;
                }
                this.inputDelay.reset(); // <<< (2) 이 줄도 다시 추가합니다.

            } else if (inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
                // 엔터 키: 선택 실행
                executeSelectedOption();
                this.inputDelay.reset(); 

            } else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                // 스페이스 키: 화면 나가기
                this.isRunning = false;
                this.inputDelay.reset(); 
            }
        }
    }

    /**
     * (4) 수정: executeSelectedOption() 메소드
     * 콘솔 출력 대신, 'optionStates'의 boolean 값을 토글(반전)시킵니다.
     */
    private void executeSelectedOption() {
        // 현재 선택된 옵션의 boolean 값을 반전시킵니다 (true -> false, false -> true)
        this.optionStates[this.selectedOption] = !this.optionStates[this.selectedOption];

        // TODO: 여기에 실제 치트 적용 로직을 구현합니다.
        // 예시:
        // boolean newState = this.optionStates[this.selectedOption];
        // switch (this.selectedOption) {
        //     case 0: // Invincibility
        //         GameState.setInvincible(newState);
        //         break;
        //     case 1: // Infinite Lives
        //         GameState.setLives(newState ? 99 : 3); // 켜지면 99, 꺼지면 기본값 3
        //         break;
        // }
    }
    
    // --- (5) 수정: Getter 메소드에 optionStates 추가 ---
    public final String[] getMenuOptions() {
        return this.menuOptions;
    }

    public final int getSelectedOption() {
        return this.selectedOption;
    }
    
    /** on/off 상태 배열을 DrawManager에 전달하기 위한 Getter */
    public final boolean[] getOptionStates() { // <<<< 새로 추가
        return this.optionStates;
    }
    // --------------------------------------------------------

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawEasterEgg(this); 
        drawManager.completeDrawing(this);
    }
}