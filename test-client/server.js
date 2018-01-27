const qald = require('./qald-7-train-multilingual.json')
const readline = require('readline')
const request = require('request')
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})

let index = 0

rl.on('line', () => {
  if (index < qald.questions.length) {
    const question = qald.questions[index++]
    request(
      {
        method: 'POST',
        uri: 'http://localhost:8181/ask-gerbil',
        qs: {
          query: question.question.filter(e => e.language === 'en')[0].string,
          lang: 'en'
        }
      },
      function (error, response, body) {
        if (error) {
          console.log('Error: ' + error)
        } else {
          console.log('ID: ' + question.id)
          console.log('Question: ' + question.question.filter(e => e.language === 'en')[0].string)
          console.log('Query: ' + question.query.sparql)
          const correctAnswer = question.answers
          console.log('Correct Answer: ' + JSON.stringify(correctAnswer, null, 4))
          const systemAnswer = JSON.parse(body).questions[0].answers[0].answer
          console.log('System Answer: ' + JSON.stringify(systemAnswer, null, 4))
          console.log('\n****************************************************************************\n')
        }
      }
    )
  } else {
    console.log('No questions remaining')
  }
})
