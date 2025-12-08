# LangChain & Agents AI: Kiến Thức & Implementation trong Clinic AI Service

## 📖 Tổng quan

Document này mô tả chi tiết các kiến thức và kỹ thuật **LangChain** + **Agents AI** đã được áp dụng để xây dựng **Clinic AI Service** - một chatbot thông minh có khả năng tư vấn và đặt lịch khám bệnh.

## 🏗️ Kiến trúc tổng thể

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   User Input    │────│  LangChain Agent │────│  External APIs  │
│  (Natural Lang) │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   Tools/Actions  │
                       │                  │
                       │ • Search doctors │
                       │ • Check slots    │
                       │ • Create booking │
                       │ • Query RAG      │
                       └──────────────────┘
```

---

## 🤖 1. LangChain Core Concepts

### 1.1 LLM (Large Language Model)
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="gpt-4o",           # Model selection
    temperature=0.1,          # Low temperature for deterministic responses
    openai_api_key=API_KEY
)
```

**Kiến thức áp dụng:**
- **GPT-4o**: Model mạnh nhất cho cả reasoning và function calling
- **Temperature = 0.1**: Giảm creativity, tăng consistency cho medical domain
- **Max tokens**: Control để tránh response quá dài

### 1.2 Agents vs Chains

**Agents** (đã sử dụng):
```python
# Agent có khả năng tự quyết định tool nào để sử dụng
agent = create_openai_functions_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)
```

**Chains** (không sử dụng):
```python
# Chains có flow cố định, không dynamic như agents
chain = LLMChain(llm=llm, prompt=prompt)
```

**Tại sao chọn Agents:**
- ✅ Dynamic tool selection
- ✅ Handle complex multi-step tasks
- ✅ Better for conversational AI
- ✅ Self-correcting capabilities

---

## 🛠️ 2. Function Calling (Tools Integration)

### 2.1 Tool Definition Pattern

```python
from langchain.tools import tool
from typing import List, Dict, Any

@tool
async def search_doctor_info(query: str) -> str:
    """
    Tìm kiếm thông tin bác sĩ theo tên, chuyên khoa, hoặc mô tả.

    Args:
        query: Từ khóa tìm kiếm (tên bác sĩ, chuyên khoa, etc.)

    Returns:
        Thông tin chi tiết về bác sĩ phù hợp
    """
    # Implementation logic
    pass
```

**Kiến thức áp dụng:**
- **@tool decorator**: Chuyển function thành LangChain tool
- **Type hints**: Giúp LLM hiểu parameters
- **Async functions**: Non-blocking I/O cho API calls
- **Comprehensive docstrings**: Hướng dẫn LLM cách sử dụng

### 2.2 Tools Implemented

#### 2.2.1 Query Tools (Read-only)
```python
@tool
async def search_doctor_info(query: str) -> str:
    # Semantic search trong vector DB + API fallback

@tool
async def check_available_slots(date: str, shift: str = None) -> str:
    # Query slot availability từ BookingService

@tool
async def recommend_medical_packages(symptoms: str) -> str:
    # AI-powered package recommendations

@tool
async def get_clinic_info(query: str) -> str:
    # RAG search trong clinic knowledge base

@tool
async def get_doctor_schedule(doctor_name: str, month: int, year: int) -> str:
    # Staff schedule lookup
```

#### 2.2.2 Command Tools (Write operations)
```python
@tool
async def create_booking(patient_info: str, slot_id: str) -> str:
    # Create booking via BookingService API
    # Parse patient info: "name:Nguyen Van A,email:a@email.com,phone:0123456789"
    # Generate fingerprint for session tracking
    # Handle errors gracefully
```

### 2.3 Tool Selection Logic

**ReAct Pattern Implementation:**
```
Thought: User wants to book appointment
Action: check_available_slots
Observation: Found available slots
Thought: Good slots available, proceed to booking
Action: create_booking
Observation: Booking created successfully
Final Answer: Provide confirmation to user
```

---

## 🧠 3. Memory Management

### 3.1 Conversation Buffer Window Memory

```python
from langchain.memory import ConversationBufferWindowMemory

memory = ConversationBufferWindowMemory(
    memory_key="chat_history",           # Key in prompt template
    return_messages=True,                # Return as message objects
    max_token_limit=2000,               # Token limit for context
    k=10                                # Keep last 10 exchanges
)
```

**Kiến thức áp dụng:**
- **Context window management**: Giữ conversation history vừa đủ
- **Token optimization**: Tránh exceed model limits
- **Session isolation**: Memory per conversation session
- **Automatic cleanup**: Remove old messages khi quá limit

### 3.2 Memory Integration

```python
# Memory được inject vào prompt template
prompt = ChatPromptTemplate.from_messages([
    ("system", system_prompt),
    MessagesPlaceholder(variable_name="chat_history"),  # ← Memory here
    ("human", "{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])

# Agent sử dụng memory
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=memory,  # ← Memory instance
    verbose=True
)
```

---

## 🔍 4. RAG (Retrieval-Augmented Generation)

### 4.1 Vector Database Integration

```python
import chromadb
from langchain_community.vectorstores import Chroma
from langchain_openai import OpenAIEmbeddings

# Initialize ChromaDB
client = chromadb.PersistentClient(path="./chroma_db")
embedding = OpenAIEmbeddings(model="text-embedding-ada-002")

# Collections for different data types
doctor_collection = client.get_or_create_collection("doctors")
package_collection = client.get_or_create_collection("medical_packages")
process_collection = client.get_or_create_collection("clinic_processes")
faq_collection = client.get_or_create_collection("faq")
```

### 4.2 Data Indexing Strategy

```python
async def add_doctor_documents(self, doctors: List[Dict[str, Any]]):
    """Index doctor information for semantic search"""

    for doctor in doctors:
        # Create rich context for better retrieval
        content = f"""
        Bác sĩ {doctor['name']}.
        Email: {doctor['email']}.
        Điện thoại: {doctor['phone']}.
        Chuyên khoa: {doctor['departmentName']}.
        Mô tả: {doctor['description']}.
        Trạng thái: {'Đang hoạt động' if doctor['active'] else 'Tạm nghỉ'}.
        """

        # Store in vector DB
        collection.add(
            documents=[content],
            metadatas=[{
                "type": "doctor",
                "id": doctor["id"],
                "name": doctor["name"],
                "department": doctor["departmentName"],
                "active": doctor["active"]
            }],
            ids=[f"doctor_{doctor['id']}"]
        )
```

### 4.3 Retrieval Implementation

```python
def search_doctors(self, query: str, n_results: int = 5) -> Dict[str, Any]:
    """Semantic search for doctors"""
    results = doctor_collection.query(
        query_texts=[query],
        n_results=n_results,
        include=["documents", "metadatas", "distances"]
    )
    return results
```

### 4.4 Hybrid Search Strategy

```python
# Combine vector search + keyword filtering
def hybrid_doctor_search(query: str):
    # 1. Vector search for semantic similarity
    vector_results = vector_store.search_doctors(query)

    # 2. API fallback for exact matches
    api_results = clinic_api.get_doctors(keyword=query)

    # 3. Merge and deduplicate results
    combined_results = merge_results(vector_results, api_results)

    return combined_results
```

---

## 📝 5. Prompt Engineering

### 5.1 System Prompt Architecture

```python
SYSTEM_PROMPT_TEMPLATE = """Bạn là trợ lý AI chuyên nghiệp của Phòng Khám Đa Khoa C46.

**VAI TRÒ CỦA BẠN:**
- Cung cấp thông tin chính xác về phòng khám, bác sĩ, gói khám và dịch vụ
- Tư vấn và hỗ trợ đặt lịch khám cho bệnh nhân
- Hướng dẫn quy trình khám bệnh và các thủ tục cần thiết

**QUY TRÌNH ĐẶT LỊCH:**
1. **Thu thập thông tin triệu chứng:** Hỏi về triệu chứng để tư vấn gói khám phù hợp
2. **Tư vấn gói khám:** Đề xuất gói khám dựa trên triệu chứng và nhu cầu
3. **Kiểm tra slot trống:** Xem lịch trống theo ngày, giờ và bác sĩ
4. **Thu thập thông tin cá nhân:** Hỏi tên, email, số điện thoại
5. **Xác nhận và đặt lịch:** Tạo booking và gửi thông tin xác nhận
6. **Hướng dẫn thêm:** Nhắc nhở về thủ tục và lưu ý khi đến khám

**LUẬT VÀNG:**
- Luôn sử dụng tools để lấy thông tin chính xác, KHÔNG được bịa đặt
- Nếu không chắc chắn, hãy hỏi lại hoặc chuyển cho nhân viên
- Ưu tiên gợi ý gói khám phù hợp với triệu chứng
- Kiểm tra slot trống trước khi đề xuất đặt lịch
- Xác nhận thông tin bệnh nhân đầy đủ trước khi tạo booking
- Gửi thông tin xác nhận chi tiết sau khi đặt lịch thành công

**THÔNG TIN PHÒNG KHÁM:**
- Tên: Phòng Khám Đa Khoa C46
- Giờ hoạt động: Thứ 2-6: 8:00-17:00, Thứ 7-CN: 7:00-12:00
- Hotline: 1900-xxxx
- Email: clinic.management.c46@gmail.com
- Địa chỉ: [97 Man Thiện, phường Tăng Nhơn Phú, TP. Hồ Chí Minh]

**CÁC GÓI KHÁM CHÍNH:**
{medical_packages_list}

**LƯU Ý QUAN TRỌNG:**
- Slot sáng: 7:00-11:00, Slot chiều: 13:00-17:00
- Cần đặt lịch trước ít nhất 24 giờ
- Mang theo CMND/CCCD và thẻ bảo hiểm (nếu có)
- Đến trước 15 phút để hoàn tất thủ tục
"""
```

### 5.2 Dynamic Prompt Generation

```python
async def build_dynamic_system_prompt(clinic_api: ClinicAPIService) -> str:
    """Generate system prompt with real-time data from database"""

    # Fetch live data from microservices
    packages = await clinic_api.get_medical_packages()

    # Format packages dynamically
    packages_list = []
    for package in packages[:10]:
        name = package.get('name', 'N/A')
        price = package.get('price', 0)
        description = package.get('description', '')[:100]

        formatted_price = f"{price:,} VND" if price > 0 else "Liên hệ"
        packages_list.append(f"- {name}: {description} - Giá: {formatted_price}")

    medical_packages_text = "\n".join(packages_list)

    # Inject into template
    return SYSTEM_PROMPT_TEMPLATE.format(medical_packages_list=medical_packages_text)
```

### 5.3 Few-shot Examples

```python
FEW_SHOT_EXAMPLES = [
    {
        "user": "Tôi bị đau răng, muốn khám",
        "assistant": "Tôi hiểu bạn đang gặp vấn đề về răng. Phòng khám chúng tôi có đội ngũ bác sĩ răng miệng chuyên nghiệp. Bạn có thể cho tôi biết thêm về triệu chứng không? Ví dụ như đau mức độ nào, răng nào bị đau, hay có các triệu chứng khác không?"
    },
    {
        "user": "Bác sĩ Nguyễn Văn A khám những ngày nào?",
        "assistant": "Bác sĩ Nguyễn Văn A là bác sĩ chuyên khoa răng miệng, làm việc tại phòng khám từ thứ 2 đến thứ 6. Để biết chính xác lịch trống, bạn muốn đặt lịch vào ngày nào trong tuần này?"
    },
    # ... more examples
]
```

---

## ⚡ 6. Caching & Performance Optimization

### 6.1 System Prompt Caching

```python
from cachetools import TTLCache

_system_prompt_cache = TTLCache(maxsize=1, ttl=3600)  # 1 hour TTL

async def create_agent_prompt(clinic_api: ClinicAPIService):
    cache_key = "system_prompt"

    # Check cache first
    if cache_key in _system_prompt_cache:
        system_prompt = _system_prompt_cache[cache_key]
    else:
        # Generate new prompt
        system_prompt = await build_dynamic_system_prompt(clinic_api)
        _system_prompt_cache[cache_key] = system_prompt

    return ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        # ... other messages
    ])
```

### 6.2 API Response Caching

```python
# Cache expensive API calls
@cached(cache=TTLCache(maxsize=100, ttl=300))  # 5 minutes
async def get_doctor_schedule(month: int, year: int):
    return await clinic_api.get_doctor_schedule(month, year)
```

---

## 🛡️ 7. Error Handling & Resilience

### 7.1 Tool Error Handling

```python
@tool
async def create_booking(patient_info: str, slot_id: str) -> str:
    try:
        # Parse and validate input
        patient_data = parse_patient_info(patient_info)

        # Validate required fields
        required_fields = ['name', 'email', 'phone']
        missing = [f for f in required_fields if f not in patient_data]
        if missing:
            return f"Thiếu thông tin: {', '.join(missing)}"

        # Call external API
        booking_id = await clinic_api.create_booking(...)

        return f"✅ Đặt lịch thành công! Mã: {booking_id}"

    except httpx.HTTPError as e:
        logger.error(f"API Error: {e}")
        return "Lỗi kết nối. Vui lòng thử lại sau."

    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return "Có lỗi xảy ra. Vui lòng liên hệ hotline."
```

### 7.2 Agent Error Recovery

```python
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    max_iterations=5,                    # Limit retry attempts
    early_stopping_method="generate",    # Stop on final answer
    handle_parsing_errors=True,          # Handle malformed responses
    callbacks=[error_callback]
)
```

### 7.3 Fallback Strategies

```python
async def build_dynamic_system_prompt(clinic_api):
    try:
        packages = await clinic_api.get_medical_packages()
        # Format from live data
    except Exception:
        # Fallback to hardcoded data
        logger.warning("Using fallback packages list")
        packages = FALLBACK_PACKAGES_LIST
```

---

## 🔌 8. Microservices Integration Patterns

### 8.1 API Client Design

```python
class ClinicAPIService:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.client = httpx.AsyncClient(timeout=10.0)

    async def get_doctors(self, keyword=None, department=None):
        """Query StaffService API"""
        params = {}
        if keyword: params['keyword'] = keyword
        if department: params['departmentId'] = department

        response = await self.client.get(
            f"{self.base_url}/api/staff",
            params=params
        )
        return response.json()['data']['content']
```

### 8.2 Circuit Breaker Pattern

```python
from httpx import Timeout
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10)
)
async def call_booking_api(self, data):
    """Call BookingService with retry logic"""
    async with self.client as client:
        response = await client.post(
            f"{self.base_url}/api/booking",
            json=data,
            timeout=Timeout(5.0)
        )
        response.raise_for_status()
        return response.json()
```

### 8.3 Session Management

```python
@tool
async def create_booking(patient_info: str, slot_id: str):
    # Generate unique session fingerprint
    fingerprint = str(uuid.uuid4())

    # Use fingerprint for booking tracking
    booking_result = await clinic_api.create_booking(
        slot_id=slot_id,
        patient_info=patient_data,
        fingerprint=fingerprint
    )

    return booking_result
```

---

## 📊 9. Monitoring & Observability

### 9.1 Agent Action Logging

```python
class ClinicAgentCallbackHandler(BaseCallbackHandler):
    def on_tool_start(self, serialized, input_str, **kwargs):
        logger.info(f"Tool started: {serialized['name']} with input: {input_str}")

    def on_tool_end(self, output, **kwargs):
        logger.info(f"Tool completed, output length: {len(output)}")

    def on_agent_action(self, action, **kwargs):
        logger.info(f"Agent selected tool: {action.tool} with params: {action.tool_input}")
```

### 9.2 Performance Metrics

```python
import time

async def run(self, user_input: str, session_id: str):
    start_time = time.time()

    try:
        result = await self.agent_executor.arun(input=user_input)

        # Log performance
        duration = time.time() - start_time
        logger.info(f"Agent response time: {duration:.2f}s for session {session_id}")

        return result

    except Exception as e:
        logger.error(f"Agent error after {time.time() - start_time:.2f}s: {e}")
        raise
```

---

## 🔄 10. Data Synchronization

### 10.1 Background Sync Jobs

```python
from apscheduler.schedulers.asyncio import AsyncIOScheduler

class DataSyncService:
    def __init__(self):
        self.scheduler = AsyncIOScheduler()

    async def start(self):
        # Sync doctors every 15 minutes
        self.scheduler.add_job(
            self._sync_doctors,
            'interval',
            minutes=15,
            id='sync_doctors'
        )

        # Sync packages every 30 minutes
        self.scheduler.add_job(
            self._sync_packages,
            'interval',
            minutes=30,
            id='sync_packages'
        )

        self.scheduler.start()

    async def _sync_doctors(self):
        """Update doctor data in vector DB"""
        doctors = await clinic_api.get_doctors()
        await vector_store.update_doctor_documents(doctors)
```

### 10.2 Real-time Updates (Future Enhancement)

```python
# Webhook endpoint for real-time updates
@app.post("/webhooks/doctor-updated")
async def doctor_updated_webhook(data: Dict[str, Any]):
    """Receive webhook when doctor data changes"""
    doctor_id = data['doctor_id']

    # Update specific doctor in vector DB
    doctor = await clinic_api.get_doctor_by_id(doctor_id)
    await vector_store.update_doctor(doctor)

    return {"status": "updated"}
```

---

## 🎯 11. Best Practices Implemented

### 11.1 Separation of Concerns
- **Agent Logic**: Pure AI reasoning in `agent_core.py`
- **Tools**: External action implementations in `tools.py`
- **Data Layer**: API clients in `clinic_api.py`
- **RAG**: Vector operations in `vector_store.py`

### 11.2 Error Boundaries
- **Tool Level**: Each tool handles its own errors
- **Agent Level**: Agent executor manages parsing errors
- **API Level**: HTTP clients handle network failures
- **System Level**: Fallbacks for critical components

### 11.3 Performance Optimization
- **Caching**: System prompts, API responses
- **Async/Await**: Non-blocking I/O everywhere
- **Connection Pooling**: HTTP client reuse
- **Memory Management**: Conversation history limits

### 11.4 Security Considerations
- **Input Validation**: Sanitize user inputs
- **API Authentication**: Service-to-service auth
- **Rate Limiting**: Prevent abuse
- **Error Masking**: Don't leak sensitive information

---

## 🚀 12. Advanced Features

### 12.1 Multi-turn Conversation Management

```python
class AgentManager:
    """Manage multiple agent instances per session"""

    def __init__(self):
        self.agents = {}

    def get_agent(self, session_id: str) -> ClinicAgent:
        if session_id not in self.agents:
            self.agents[session_id] = ClinicAgent()
        return self.agents[session_id]
```

### 12.2 Contextual Tool Selection

```python
# Agent learns from conversation context
# Example: If user mentioned "răng" earlier,
# agent prioritizes dental-related tools
```

### 12.3 Dynamic Tool Discovery

```python
# Future: Tools can be added/removed at runtime
# based on available microservices
def discover_tools():
    available_services = check_service_health()
    tools = []

    if 'staff-service' in available_services:
        tools.append(search_doctor_info)

    if 'booking-service' in available_services:
        tools.append(create_booking)

    return tools
```

---

## 📈 13. Scaling Considerations

### 13.1 Horizontal Scaling
- **Stateless Agents**: Each request can go to any instance
- **Shared Vector DB**: ChromaDB can be externalized
- **Redis for Sessions**: Move memory to Redis for multi-instance

### 13.2 Load Balancing
- **API Gateway**: Distribute requests across instances
- **Circuit Breakers**: Prevent cascade failures
- **Rate Limiting**: Per user and per service

### 13.3 Monitoring & Alerting
- **Response Times**: Track agent performance
- **Error Rates**: Monitor tool failures
- **Resource Usage**: Memory, CPU, API calls
- **User Satisfaction**: Conversation success metrics

---

## 🎓 14. Lessons Learned

### 14.1 Key Insights
1. **Prompt Engineering is Critical**: Well-crafted prompts > Complex logic
2. **Error Handling Matters**: Users expect graceful failures
3. **Caching is Essential**: Balance freshness vs performance
4. **Async is Non-negotiable**: Medical domain needs responsiveness
5. **Testing is Hard**: AI behavior is non-deterministic

### 14.2 Challenges Overcome
- **Tool Selection Logic**: GPT-4o needs clear tool descriptions
- **Memory Management**: Token limits require careful history pruning
- **Real-time Data**: Balance between cached and live data
- **Error Recovery**: Agent must handle API failures gracefully
- **Performance**: Optimize for sub-second responses

---

## 📚 15. References & Further Reading

### Core LangChain Documentation
- [LangChain Agents](https://python.langchain.com/docs/modules/agents/)
- [Function Calling](https://python.langchain.com/docs/modules/agents/toolkits/openai_functions)
- [Memory Management](https://python.langchain.com/docs/modules/memory/)

### Advanced Patterns
- [ReAct Pattern](https://arxiv.org/abs/2210.03629)
- [RAG Implementation](https://arxiv.org/abs/2005.11401)
- [Prompt Engineering Guide](https://github.com/dair-ai/Prompt-Engineering-Guide)

### Production Deployment
- [LangChain Production](https://python.langchain.com/docs/guides/productionization)
- [Monitoring AI Systems](https://christophergs.com/blog/monitoring-ai-systems)
- [Building Reliable AI Agents](https://www.anthropic.com/news/building-reliable-ai-agents)

---

## 🎯 Conclusion

Clinic AI Service demonstrates a production-ready implementation of modern AI Agent architecture using LangChain, combining:

- **Advanced Reasoning**: GPT-4o with function calling
- **Real-time Data**: Dynamic system prompts from live databases
- **Robust Integration**: Microservices with error handling
- **Performance Optimization**: Caching and async patterns
- **Scalable Architecture**: Ready for production deployment

The system successfully handles complex medical appointment booking conversations while maintaining accuracy, reliability, and user experience.
