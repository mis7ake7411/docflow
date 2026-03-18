const axios = require('axios');

async function testLogin() {
  const loginUrl = 'https://docflow-hoing.zeabur.app/api/auth/login';
  const loginData = {
    email: 'test@example.com',
    password: 'Test1234!'
  };

  try {
    const response = await axios.post(loginUrl, loginData);
    if (response.status === 200 && response.data.success) {
      console.log('Login test passed:', response.data);
    } else {
      console.error('Login test failed:', response.data);
    }
  } catch (error) {
    console.error('Error during login test:', error.response ? error.response.data : error.message);
  }
}

testLogin();