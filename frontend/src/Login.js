import React from 'react';
import TelegramLoginButton from 'react-telegram-login';
import './Login.css';

const Login = props =>
      props.username ?
		  (<div>{props.username} <button onClick={ e => props.onLogoff()}>Logoff</button></div>)
	  	   : <TelegramLoginButton dataOnauth={tele => props.onLogin(tele)} botName="lgc_semaphore_bot" />	  

export default Login;
