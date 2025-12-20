# luồng hoạt động & Cơ chế AIService

Tài liệu này giải thích chi tiết về kiến trúc, luồng xử lý và cơ chế hoạt động của AIService, đặc biệt tập trung vào LangGraph, RAG và cách các thành phần kết nối với nhau.

## 1. Tổng quan kiến trúc

Hệ thống AI Service được xây dựng dựa trên các thành phần chính:
- **FastAPI**: Cung cấp REST API để giao tiếp với Client.
- **LangGraph**: Quản lý luồng hội thoại và trạng thái của Agent.
- **Tools**: Các công cụ (Function calling) để Agent tương tác với dữ liệu và thực hiện hành động.
- **RAG (Retrieval Augmented Generation)**: Sử dụng Postgres (pgvector) để lưu trữ và tìm kiếm vector.
- **ClinicAPIService**: Gateway để gọi sang các Microservice khác (Staff, MedicalPackage, Booking...).

---

## 2. Luồng đi của một Request (Request Flow)

Khi một yêu cầu (user message) được gửi đến, nó sẽ đi qua các bước sau:

### Bước 1: API Entry Point
User gọi API `POST /api/ai/chat` với body `{ "message": "...", "session_id": "..." }`.
- `main.py` nhận request và chuyển cho `AgentManager`.

### Bước 2: Agent Manager & LangChain Initialization
- `AgentManager` tìm hoặc khởi tạo `LangGraphAgent` cho session đó.
- Nếu là session mới, lịch sử trò chuyện (Memory) được khởi tạo.

### Bước 3: LangGraph Execution Loop
LangGraph hoạt động theo mô hình đồ thị trạng thái (`StateGraph`).

1.  **Node "agent"**:
    -   Đầu vào: Danh sách message hiện tại (User message + History).
    -   Xử lý: Gửi toàn bộ context + danh sách **Tools** (được bind) sang LLM (OpenAI).
    -   LLM Suy luận: Dựa trên System Prompt và User Message, LLM quyết định:
        -   *Trường hợp A*: Trả lời trực tiếp bằng text.
        -   *Trường hợp B*: Cần gọi một Tool (ví dụ `check_available_slots`).

2.  **Edge "should_continue" (Conditional Edge)**:
    -   LangGraph kiểm tra output của LLM.
    -   Nếu LLM trả về Text -> Đi đến **END** -> Trả response cho user.
    -   Nếu LLM trả về `tool_calls` -> Đi đến Node **"tools"**.

3.  **Node "tools"**:
    -   Thực thi tool tương ứng mà LLM yêu cầu (ví dụ: chạy hàm Python `check_available_slots`).
    -   Kết quả của tool (ví dụ: danh sách slot trống) được thêm vào danh sách message dưới dạng `ToolMessage`.
    -   Luồng quay lại Node **"agent"**.

4.  **Node "agent" (Lần 2)**:
    -   LLM nhận được kết quả từ tool.
    -   LLM tổng hợp thông tin và sinh ra câu trả lời cuối cùng cho người dùng (Natural Language).

---

## 3. Cơ chế RAG (Retrieval Augmented Generation)

User có thắc mắc về: "RAG init dữ liệu như thế nào, chuyển thành vector, lưu và lấy ra như thế nào?".

### 3.1. Tại sao cần RAG & Vector?
LLM (GPT) không biết dữ liệu riêng của phòng khám (lịch bác sĩ, danh sách gói khám mới nhất). Chúng ta cần cung cấp thông tin này cho LLM. Thay vì training lại model, ta dùng RAG: tìm kiếm thông tin liên quan và "mớm" cho LLM ngay trong prompt.

Vector là gì? Là biểu diễn số học (mảng số thực, ví dụ `[0.1, -0.5, 0.9...]`) của một đoạn văn bản. Hai đoạn văn bản có ý nghĩa giống nhau sẽ có vector "gần" nhau trong không gian số học.

### 3.2. Quá trình Init & Vector hóa (Data Loading)
Quá trình này được thực hiện bởi `DataLoader` và `PGVectorStore`:

1.  **Fetch Data**: `DataLoader` gọi `ClinicAPIService` để lấy dữ liệu thô từ DB chính (MySQL) của các service khác.
    -   Lấy danh sách Bác sĩ.
    -   Lấy danh sách Gói khám.
    -   Lấy Quy trình & FAQ (dữ liệu tĩnh).

2.  **Chunking & Formatting**: Dữ liệu thô được chuyển thành văn bản có ý nghĩa (Document).
    -   *Ví dụ*: `{name: "Bs A", dep: "Tim mạch"}` -> "Bác sĩ A. Chuyên khoa tim mạch..."

3.  **Embedding (Vector hóa)**:
    -   Text được gửi qua `OpenAIEmbeddings`.
    -   Kết quả trả về là một vector (1536 chiều).

4.  **Storage**:
    -   Lưu vào bảng PostgreSQL có extension `pgvector`.
    -   Cấu trúc bảng: `id | collection_name | content | metadata | embedding`.

### 3.3. Khi nào dữ liệu được lấy ra?
Khi một Tool cần tìm kiếm thông tin (ví dụ `search_doctor_info`), nó sẽ:
1.  Nhận query từ user (ví dụ: "bác sĩ chữa đau tim").
2.  Chuyển query này thành vector (query vector).
3.  Thực hiện **Similarity Search** trong DB: Tìm những vector bác sĩ gần nhất với vector query (Cosine Similarity).
4.  Lấy thông tin text gốc của các bác sĩ tìm được trả về cho Agent.

---

## 4. Chi tiết các Use Cases trong hệ thống

### Case 1: Hỗ trợ bệnh nhân đặt lịch
**Luồng đi:**
1.  **User**: "Tôi muốn đặt lịch khám tim vào sáng mai."
2.  **Agent**: Nhận diện intent -> Gọi tool `check_available_slots(date="ngày mai", shift="MORNING")`.
3.  **Tool**: Gọi `ClinicAPI` lấy danh sách slot -> Trả về JSON/Text các slot trống.
4.  **Agent**: Đọc kết quả và trả lời: "Có slot lúc 8:00 và 9:00, bạn chọn giờ nào?"
5.  **User**: "8 giờ đi."
6.  **Agent**: Gọi tool `create_booking(slot_id=..., patient_info=...)`.
7.  **Tool**: Gọi `POST /booking` sang BookingService -> Trả về Booking ID.
8.  **Agent**: "Đặt lịch thành công, mã của bạn là..."

### Case 2: Tư vấn gói khám (Logic-based vs RAG)
Hệ thống hiện tại có 2 cơ chế, nhưng **ưu tiên Logic-based** cho tư vấn bệnh:

1.  **Tư vấn dựa trên triệu chứng (`recommend_medical_packages`)**:
    -   Không dùng Vector Search thuần túy.
    -   Sử dụng **MedicalSymptomAnalyzer** (được code trong `medical_analyzer.py`).
    -   Cơ chế: Phân tích keyword, regex, và luật y khoa (Rule-based) để so khớp triệu chứng với danh mục gói khám.
    -   Lý do: Để đảm bảo độ chính xác về mặt y khoa (Clinical Accuracy) cao hơn so với tìm kiếm tương đồng ngữ nghĩa đơn thuần.

2.  **Tìm kiếm thông tin gói khám (`list_medical_packages`)**:
    -   Hiện tại đang dùng API search (keyword matching).
    -   Tuy nhiên, dữ liệu gói khám **đã được Vector hóa** trong collection `medical_packages`. Nếu muốn tìm kiếm "gói khám cho người hay bị mệt", ta hoàn toàn có thể chuyển sang dùng `vector_store.similarity_search` để tìm.

### Case 3: Hỏi đáp thông tin phòng khám (RAG thuần)
**Luồng đi:**
1.  **User**: "Phòng khám có chỗ đậu xe không?"
2.  **Agent**: Gọi tool `get_clinic_info`.
3.  **Tool**:
    -   Embed câu hỏi "Phòng khám có chỗ đậu xe không?".
    -   Search trong vector DB (collection `faq` và `clinic_processes`).
    -   Tìm thấy đoạn văn: "Có, phòng khám có khu đậu xe miễn phí...".
4.  **Agent**: Tổng hợp và trả lời user.

---

## Tóm tắt các thành phần kết nối

| Thành phần | Vai trò | Kết nối với |
| :--- | :--- | :--- |
| **AgentManager** | Quản lý vòng đời AI session | FastAPI (`main.py`) |
| **LangGraph** | Bộ não điều khiển luồng, ra quyết định | LLM (OpenAI) & Tools |
| **Tools (`tools.py`)** | Tay chân thực hiện hành động | LangGraph & ClinicAPIService & PGVectorStore |
| **PGVectorStore** | Bộ nhớ dài hạn (Knowledge Base) | Postgres DB |
| **DataLoader** | Nạp dữ liệu vào bộ nhớ | ClinicAPIService -> PGVectorStore |
| **MedicalAnalyzer** | Bộ não phụ chuyên môn y khoa | Tool `recommend_medical_packages` |

Bằng cách kết hợp **LangGraph** (Logic luồng) + **RAG** (Tìm kiếm thông tin) + **Rule-based Analyzer** (Chuyên môn y khoa), hệ thống đảm bảo vừa linh hoạt trong giao tiếp, vừa chính xác trong tư vấn y tế.
