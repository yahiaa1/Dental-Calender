import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyAwUFN0EVb99R7PUY_ZAZPblmx2yKAOQ5k",
  authDomain: "dental-calender-ef240.firebaseapp.com",
  projectId: "dental-calender-ef240",
  storageBucket: "dental-calender-ef240.firebasestorage.app",
  messagingSenderId: "931136384586",
  appId: "1:931136384586:web:c3087ed03afcddea5f00b3"
};

// Initialize Firebase
export const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
