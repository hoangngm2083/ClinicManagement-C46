# Fixes Applied for AI Service Issues

## 🔧 Issues Fixed

### 1. ✅ Vector Store Connection (pgvector extension)

**Problem**: 
```
ERROR: type "vector" does not exist
```

**Solution**:
- Updated `docker-compose.yml` to use `pgvector/pgvector:pg15` image instead of `postgres:15-alpine`
- Created extension in database:
  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```

**Status**: ✅ Fixed - Extension installed successfully (vector 0.8.1)

### 2. ✅ AI Service Response Empty

**Problem**: 
- Agent executor returns response but `output` field is empty
- Response structure: `{"response": "", "suggested_actions": ["general_help"], ...}`

**Root Cause**:
- LangGraph `create_agent_executor` returns response with structure:
  ```python
  {
    "input": "...",
    "chat_history": [],
    "agent_outcome": AgentFinish(return_values={"output": "..."}),
    "intermediate_steps": [...]
  }
  ```
- Code was trying to get `response.get("output", "")` but output is actually in `agent_outcome.return_values["output"]`

**Solution**:
- Updated `AIService/app/agents/langgraph_agent.py` to correctly extract output:
  ```python
  agent_outcome = response.get("agent_outcome")
  if agent_outcome and hasattr(agent_outcome, "return_values"):
      return_values = agent_outcome.return_values
      if isinstance(return_values, dict):
          final_answer = return_values.get("output", "")
  ```

**Status**: ✅ Fixed - Code updated, needs container rebuild

## 📋 Next Steps

1. **Rebuild AI Service container** to apply code changes:
   ```bash
   docker-compose build ai-service
   docker-compose up -d ai-service
   ```

2. **Verify fixes**:
   ```bash
   # Test vector store
   curl http://localhost:8000/health
   
   # Test chat endpoint
   curl -X POST http://localhost:8000/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "Xin chào", "session_id": "test"}'
   ```

3. **Expected Results**:
   - Health endpoint should show `"vector_store": true`
   - Chat endpoint should return proper response with text, not empty string

## 🔍 Verification Commands

```bash
# Check pgvector extension
docker exec postgres psql -U booking -d vector_db -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';"

# Check vector store health
docker exec ai-service python3 -c "from app.rag.pgvector_store import PGVectorStore; vs = PGVectorStore(); print('Health:', vs.health_check())"

# Test agent response extraction
docker exec ai-service python3 -c "
import asyncio, sys
sys.path.insert(0, '/app')
from app.agents.langgraph_agent import AgentManager
from app.services.clinic_api import ClinicAPIService
from app.rag.pgvector_store import PGVectorStore
async def test():
    clinic_api = ClinicAPIService()
    vector_store = PGVectorStore()
    manager = AgentManager()
    await manager.initialize_default_agent(clinic_api, vector_store)
    result = await manager.run_agent('Xin chào', 'test')
    print('Response:', result.get('response', '')[:100])
asyncio.run(test())
"
```

## 📝 Files Modified

1. `docker-compose.yml` - Updated PostgreSQL image to `pgvector/pgvector:pg15`
2. `AIService/app/agents/langgraph_agent.py` - Fixed output extraction from LangGraph response
3. `docker/postgres/Dockerfile` - Created (uses pgvector base image)

## ✅ Summary

- **Vector Store**: ✅ Fixed - Extension installed
- **Response Extraction**: ✅ Fixed - Code updated (needs rebuild)
- **Container**: ⚠️ Needs rebuild to apply code changes

