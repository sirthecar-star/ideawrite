package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.IdeaMemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [IdeaMemo::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spark_idea_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.ideaDao()?.insertAll(getInitialSampleIdeas())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getInitialSampleIdeas(): List<IdeaMemo> {
            val now = System.currentTimeMillis()
            return listOf(
                IdeaMemo(
                    id = 1,
                    title = "⚡ 음성 기반 스마트 일정 & 아이디어 비서",
                    content = "운전 중이나 이동할 때 떠오르는 생각을 마이크 버튼 하나로 녹음하면 자동으로 태그(#음성, #기획)를 분류하고 할 일 목록으로 변환해주는 서비스 구상.\n- 위젯을 통한 1초 즉시 녹음\n- 음성 인식 후 핵심 키워드 자동 하이라이트\n- 태그별 모아보기 제공",
                    category = "💡 창작/기획",
                    tags = listOf("아이디어", "음성비서", "생산성", "기획"),
                    colorHex = "#FEF3C7",
                    importance = 5,
                    isPinned = true,
                    isFavorite = true,
                    createdAt = now - 1000 * 60 * 60 * 5,
                    updatedAt = now - 1000 * 60 * 60 * 5,
                    isVoiceRecorded = true
                ),
                IdeaMemo(
                    id = 2,
                    title = "📱 로컬 우선(Offline-First) 동기화 아키텍처",
                    content = "Room DB를 단일 진실 공급원(Single Source of Truth)으로 사용하고, 백그라운드 워커를 통해 네트워크 연결 시 원활하게 상태를 맞추는 방안 연구.\n- 충돌 해결(Conflict Resolution) 전략 수립\n- 암호화된 로컬 캐시 적용 검토",
                    category = "🛠️ 개발/IT",
                    tags = listOf("Android", "Room", "아키텍처", "개발"),
                    colorHex = "#E0F2FE",
                    importance = 4,
                    isPinned = true,
                    isFavorite = false,
                    createdAt = now - 1000 * 60 * 60 * 24,
                    updatedAt = now - 1000 * 60 * 60 * 12
                ),
                IdeaMemo(
                    id = 3,
                    title = "☕ 미니멀리즘 데스크 셋업 & 루틴",
                    content = "아침 30분 모닝 페이지 작성하기:\n1. 떠오르는 생각 가감 없이 3줄 적기\n2. 오늘의 가장 중요한 1가지 아이디어 선정\n3. 집중 타이머 시작 전 책상 정리",
                    category = "🎯 목표/습관",
                    tags = listOf("모닝루틴", "습관", "미니멀"),
                    colorHex = "#D1FAE5",
                    importance = 3,
                    isPinned = false,
                    isFavorite = true,
                    createdAt = now - 1000 * 60 * 60 * 48,
                    updatedAt = now - 1000 * 60 * 60 * 30
                ),
                IdeaMemo(
                    id = 4,
                    title = "🚀 다음 분기 프로젝트 브레인스토밍",
                    content = "팀원들과 함께할 피치 데이 아이디어:\n- AI를 활용한 고객 피드백 감성 분석\n- 사내 태그 기반 지식 공유 저장소 구축\n- 발표 슬라이드 초안 작성 필요",
                    category = "💼 업무/비즈니스",
                    tags = listOf("프로젝트", "회의", "비즈니스", "AI"),
                    colorHex = "#EEF2FF",
                    importance = 5,
                    isPinned = false,
                    isFavorite = false,
                    createdAt = now - 1000 * 60 * 60 * 72,
                    updatedAt = now - 1000 * 60 * 60 * 72
                ),
                IdeaMemo(
                    id = 5,
                    title = "📚 읽고 싶은 도서 & 연구 주제",
                    content = "- 생각이 돈이 되는 순간 (창의성 관련)\n- 원씽 (The One Thing)\n- 인간 중심 인공지능 인터페이스 디자인 연구",
                    category = "📚 학습/연구",
                    tags = listOf("독서", "학습", "자기계발"),
                    colorHex = "#F3E8FF",
                    importance = 2,
                    isPinned = false,
                    isFavorite = false,
                    createdAt = now - 1000 * 60 * 60 * 100,
                    updatedAt = now - 1000 * 60 * 60 * 100
                )
            )
        }
    }
}
