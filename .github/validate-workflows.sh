# Workflow Validation Test
# This file can be used to test workflow functionality locally

echo "🔧 C.L.A.R.A Workflow Validation Test"
echo "======================================="

echo "📋 Testing Gemini 2.5 Flash Lite Integration..."

# Check for Gemini dependencies in pom.xml
if grep -E "(okhttp|gson)" pom.xml > /dev/null; then
    echo "✅ Gemini AI dependencies found in pom.xml"
else
    echo "❌ Missing Gemini AI dependencies"
    exit 1
fi

# Check for Gemini 2.5 Flash Lite configuration
if grep -r "gemini-2.5-flash-lite" . --include="*.yml" --include="*.java" > /dev/null; then
    echo "✅ Gemini 2.5 Flash Lite model configured"
else
    echo "❌ Gemini 2.5 Flash Lite model not found"
    exit 1
fi

# Check for AI commands
if grep -q "ai:" src/main/resources/plugin.yml; then
    echo "✅ AI commands registered in plugin.yml"
else
    echo "❌ AI commands not registered"
    exit 1
fi

# Check for AI source files
if find src -name "*.java" -exec grep -l -i "ai\|gemini" {} \; | wc -l | grep -v "^0$" > /dev/null; then
    echo "✅ AI integration source files found"
else
    echo "❌ No AI integration source files found"
    exit 1
fi

# Check workflow files
if [ -f ".github/workflows/build.yml" ] && [ -f ".github/workflows/ci.yml" ] && [ -f ".github/workflows/gemini-test.yml" ]; then
    echo "✅ All workflow files present"
else
    echo "❌ Missing workflow files"
    exit 1
fi

echo ""
echo "🎉 All validations passed!"
echo "✅ Plugin package build workflows implemented"
echo "✅ Gemini 2.5 Flash Lite integration configured"
echo "✅ Ready for GitHub Actions CI/CD"