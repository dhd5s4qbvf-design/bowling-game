export interface FrameResult {
  frameNumber: number;
  rolls: number[];
  strike: boolean;
  spare: boolean;
  complete: boolean;
  score: number | null;
  runningTotal: number | null;
}

export interface GameState {
  rolls: number[];
  frames: FrameResult[];
  totalScore: number;
  gameOver: boolean;
}
